package org.codesystem.server.service.packages;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.DeploymentRepository;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.request.packages.AddNewPackageRequest;
import org.codesystem.server.request.packages.UpdatePackageContentRequest;
import org.codesystem.server.request.packages.UpdatePackageRequest;
import org.codesystem.server.response.general.ApiError;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.response.packages.GetAllPackagesResponse;
import org.codesystem.server.response.packages.GetPackageResponse;
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


    public ResponseEntity<ApiResponse> getAllPackages() {
        return ResponseEntity.ok().body(new GetAllPackagesResponse(packageRepository.findAll()));
    }

    public ResponseEntity<ApiResponse> getPackage(String packageUUID) {
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Package not found"));
        }
        return ResponseEntity.ok().body(new GetPackageResponse(packageEntity));
    }

    public ResponseEntity<ApiResponse> addNewNewPackage(AddNewPackageRequest addNewPackageRequest, MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty() || multipartFile.getContentType() == null || !multipartFile.getContentType().equalsIgnoreCase("application/zip")) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid zip-file"));
        }
        if (addNewPackageRequest == null) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid Request"));
        }
        String calculatedChecksum;
        try {
            calculatedChecksum = cryptoUtility.calculateChecksum(multipartFile.getInputStream());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new ApiError("Error when reading zip-file and creating checksum"));
        }
        if (!calculatedChecksum.equals(addNewPackageRequest.getPackageChecksum())) {
            return ResponseEntity.badRequest().body(new ApiError("Checksum mismatch"));
        }

        PackageEntity packageEntity = new PackageEntity();
        packageEntity.setName(addNewPackageRequest.getPackageName());
        packageEntity.setChecksumPlaintext(calculatedChecksum);
        packageEntity.setTargetOperatingSystem(addNewPackageRequest.getOperatingSystem());
        packageEntity = packageRepository.save(packageEntity);

        new File("/opt/OPD/Packages").mkdirs();
        try (FileOutputStream fileOutputStream = new FileOutputStream("/opt/OPD/Packages/" + packageEntity.getUuid() + "_plaintext")) {
            InputStream inputStream = multipartFile.getInputStream();
            byte[] inputStreamByte = inputStream.readNBytes(1024);
            while (inputStreamByte.length != 0) {
                fileOutputStream.write(inputStreamByte);
                inputStreamByte = inputStream.readNBytes(1024);
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new ApiError("Error when storing file"));
        }

        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageRepository.save(packageEntity);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<ApiResponse> updatePackage(UpdatePackageRequest updatePackageRequest, String packageUUID) {
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Package not found"));
        }
        if (updatePackageRequest.getPackageName() != null && !updatePackageRequest.getPackageName().equals(packageEntity.getName())) {
            packageEntity.setName(updatePackageRequest.getPackageName());
        }
        packageRepository.save(packageEntity);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<ApiResponse> deletePackage(String packageUUID) {
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Package not found"));
        }
        if (packageEntity.getPackageStatusInternal() == PackageStatusInternal.PROCESSING) {
            return ResponseEntity.badRequest().body(new ApiError("Cannot delete package while being processed"));
        }
        if (packageEntity.getPackageStatusInternal() == PackageStatusInternal.MARKED_AS_DELETED) {
            return ResponseEntity.badRequest().body(new ApiError("Package already marked for deletion"));
        }

        packageEntity.setPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        deploymentRepository.deleteDeploymentsForPackage(packageEntity);
        packageRepository.save(packageEntity);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<ApiResponse> updatePackageContent(UpdatePackageContentRequest updatePackageRequest, MultipartFile multipartFile, String packageUUID) {
        PackageEntity packageEntity = packageRepository.findFirstByUuid(packageUUID);
        if (packageEntity == null) {
            return ResponseEntity.badRequest().body(new ApiError("Package not found"));
        }

        if (multipartFile == null || multipartFile.isEmpty() || multipartFile.getContentType() == null || !multipartFile.getContentType().equalsIgnoreCase("application/zip")) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid zip-file"));
        }
        if (updatePackageRequest == null) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid Request"));
        }
        String calculatedChecksum;
        try {
            calculatedChecksum = cryptoUtility.calculateChecksum(multipartFile.getInputStream());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new ApiError("Error when reading zip-file and creating checksum"));
        }
        if (!calculatedChecksum.equals(updatePackageRequest.getPackageChecksum())) {
            return ResponseEntity.badRequest().body(new ApiError("Checksum mismatch"));
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream("/opt/OPD/Packages/" + packageEntity.getUuid() + "_plaintext")) {
            InputStream inputStream = multipartFile.getInputStream();
            byte[] inputStreamByte = inputStream.readNBytes(1024);
            while (inputStreamByte.length != 0) {
                fileOutputStream.write(inputStreamByte);
                inputStreamByte = inputStream.readNBytes(1024);
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(new ApiError("Error when storing file"));
        }

        packageEntity.setPackageStatusInternal(PackageStatusInternal.UPLOADED);
        packageRepository.save(packageEntity);
        deploymentRepository.resetDeploymentsForPackage(packageEntity);
        return ResponseEntity.ok().build();
    }
}
