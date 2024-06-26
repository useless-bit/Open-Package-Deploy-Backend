package org.codesystem.server.service.packages;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.ServerApplication;
import org.codesystem.server.Variables;
import org.codesystem.server.entity.GroupEntity;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.GroupRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.packages.PackageAddNewRequest;
import org.codesystem.server.request.packages.PackageUpdateContentRequest;
import org.codesystem.server.request.packages.PackageUpdateRequest;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.packages.PackageInfoListResponse;
import org.codesystem.server.response.packages.PackageInfoResponse;
import org.codesystem.server.service.server.LogService;
import org.codesystem.server.utility.CryptoUtility;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ManagementPackageService {
    private final PackageRepository packageRepository;
    private final CryptoUtility cryptoUtility;
    private final DeploymentRepository deploymentRepository;
    private final GroupRepository groupRepository;
    private final LogService logService;

    public ResponseEntity<ApiResponse> getAllPackages() {
        return ResponseEntity.ok().body(new PackageInfoListResponse(packageRepository.findAll()));
    }

    public ResponseEntity<ApiResponse> getPackage(String packageUUID) {
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_PACKAGE));
        }
        return ResponseEntity.ok().body(new PackageInfoResponse(packageEntity));
    }

    public ResponseEntity<ApiResponse> addNewNewPackage(PackageAddNewRequest packageAddNewRequest, MultipartFile multipartFile) {
        if (packageAddNewRequest.getPackageName() == null || packageAddNewRequest.getPackageName().isBlank()
                || packageAddNewRequest.getOperatingSystem() == null || packageAddNewRequest.getOperatingSystem() == OperatingSystem.UNKNOWN) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }

        if (multipartFile == null || multipartFile.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_ZIP_FILE));
        } else {
            String contentType = multipartFile.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase(Variables.CONTENT_TYPE_ZIP_FILE)) {
                return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_ZIP_FILE));
            }
        }

        String calculatedChecksum;
        try {
            calculatedChecksum = cryptoUtility.calculateChecksum(multipartFile.getInputStream());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.PACKAGE_ERROR_ZIP_FILE_CHECKSUM));
        }
        if (!calculatedChecksum.equals(packageAddNewRequest.getPackageChecksum())) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.PACKAGE_ERROR_CHECKSUM_MISMATCH));
        }

        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setName(packageAddNewRequest.getPackageName().trim());
        packageEntity.setChecksumPlaintext(calculatedChecksum);
        packageEntity.setTargetOperatingSystem(packageAddNewRequest.getOperatingSystem());
        packageEntity.setExpectedReturnValue(packageAddNewRequest.getExpectedReturnValue());
        packageEntity = packageRepository.save(packageEntity);

        if (!savePackage(multipartFile, packageEntity)) {
            packageRepository.delete(packageEntity);
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_CANNOT_STORE_FILE));
        }

        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageRepository.save(packageEntity);
        logService.addEntry(Severity.INFO, "New Package for " + packageEntity.getTargetOperatingSystem() + " added and awaiting processing: " + packageEntity.getName() + " | " + packageEntity.getUuid());
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<ApiResponse> updatePackage(PackageUpdateRequest packageUpdateRequest, String packageUUID) {
        if (packageUpdateRequest == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_REQUEST));
        }
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_PACKAGE));
        }
        if (packageUpdateRequest.getPackageName() != null && !packageUpdateRequest.getPackageName().isBlank() && !packageUpdateRequest.getPackageName().equals(packageEntity.getName())) {
            packageEntity.setName(packageUpdateRequest.getPackageName().trim());
        }
        packageEntity.setExpectedReturnValue(packageUpdateRequest.getExpectedReturnValue());
        packageRepository.save(packageEntity);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<ApiResponse> deletePackage(String packageUUID) {
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_PACKAGE));
        }
        if (packageEntity.getPackageStatusInternal() == PackageStatusInternal.PROCESSING) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.PACKAGE_ERROR_CANNOT_DELETE_PACKAGE_PROCESSING));
        }
        if (packageEntity.getPackageStatusInternal() == PackageStatusInternal.MARKED_AS_DELETED) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.PACKAGE_ERROR_ALREADY_DELETED));
        }

        packageEntity.setPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        for (GroupEntity group : packageEntity.getGroups()) {
            group.removePackage(packageEntity);
            groupRepository.save(group);
        }
        deploymentRepository.deleteDeploymentsForPackage(packageEntity);
        packageRepository.save(packageEntity);
        logService.addEntry(Severity.INFO, "Marked Package for deletion: " + packageEntity.getName() + " | " + packageEntity.getUuid());
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<ApiResponse> updatePackageContent(PackageUpdateContentRequest packageUpdateContentRequest, MultipartFile multipartFile, String packageUUID) {
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_NO_PACKAGE));
        }

        if (multipartFile == null || multipartFile.isEmpty() || multipartFile.getContentType() == null) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_ZIP_FILE));
        } else {
            String contentType = multipartFile.getContentType();
            if (contentType == null || contentType.isBlank() || !contentType.equalsIgnoreCase(Variables.CONTENT_TYPE_ZIP_FILE)) {
                return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_INVALID_ZIP_FILE));
            }
        }

        String calculatedChecksum;
        try {
            calculatedChecksum = cryptoUtility.calculateChecksum(multipartFile.getInputStream());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.PACKAGE_ERROR_ZIP_FILE_CHECKSUM));
        }
        if (!calculatedChecksum.equals(packageUpdateContentRequest.getPackageChecksum())) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.PACKAGE_ERROR_CHECKSUM_MISMATCH));
        }

        if (!savePackage(multipartFile, packageEntity)) {
            return ResponseEntity.badRequest().body(new ApiError(Variables.ERROR_RESPONSE_CANNOT_STORE_FILE));
        }

        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageEntity.setChecksumPlaintext(packageUpdateContentRequest.getPackageChecksum());
        packageRepository.save(packageEntity);
        deploymentRepository.resetDeploymentsForPackage(packageEntity);
        logService.addEntry(Severity.INFO, "Package content updated and awaiting processing: " + packageEntity.getName() + " | " + packageEntity.getUuid());
        return ResponseEntity.ok().build();
    }

    private boolean savePackage(MultipartFile multipartFile, PackageEntity packageEntity) {
        new File(ServerApplication.PACKAGE_LOCATION).mkdirs();
        try (FileOutputStream fileOutputStream = new FileOutputStream(ServerApplication.PACKAGE_LOCATION + packageEntity.getUuid() + "_plaintext")) {
            InputStream inputStream = multipartFile.getInputStream();
            byte[] inputStreamByte = inputStream.readNBytes(1024);
            while (inputStreamByte.length != 0) {
                fileOutputStream.write(inputStreamByte);
                inputStreamByte = inputStream.readNBytes(1024);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
