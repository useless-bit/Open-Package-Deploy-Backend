package org.codesystem.server.service.scheduled;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.ServerApplication;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.log.Severity;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.service.server.LogService;
import org.codesystem.server.utility.CryptoUtility;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PackageEncryptor {
    private final PackageRepository packageRepository;
    private final CryptoUtility cryptoUtility;
    private final LogService logService;

    private static final String WITH_ERROR = " with Error: ";

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 1)
    @Async("encryptPackageTask")
    public void encryptPackage() {
        PackageEntity packageEntity = packageRepository.findFirstByPackageStatusInternal(PackageStatusInternal.UPLOADED);
        if (packageEntity != null) {
            encryptPackage(packageEntity);
        }
    }

    private void encryptPackage(PackageEntity packageEntity) {
        // check if file exists
        Path basePath = Paths.get(ServerApplication.PACKAGE_LOCATION);
        File plaintextFile = new File(basePath + File.separator + packageEntity.getUuid() + "_plaintext");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSING);
        packageEntity = packageRepository.save(packageEntity);
        if (!plaintextFile.exists() || !plaintextFile.isFile()) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR_FILE_NOT_FOUND);
            logService.addEntry(Severity.ERROR, "Failed to locate uploaded file while processing Package: " + packageEntity.getName() + " | " + packageEntity.getUuid());
            finish(packageEntity, plaintextFile, null);
            return;
        }


        // compare checksum of plaintext file
        try (FileInputStream fileInputStream = new FileInputStream(plaintextFile)) {
            if (!cryptoUtility.calculateChecksum(fileInputStream).equals(packageEntity.getChecksumPlaintext())) {
                packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR_CHECKSUM_MISMATCH);
                logService.addEntry(Severity.ERROR, "Detected checksum mismatch for uploaded file while processing Package: " + packageEntity.getName() + " | " + packageEntity.getUuid());
                finish(packageEntity, plaintextFile, null);
                return;
            }
        } catch (Exception e) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR_CHECKSUM_MISMATCH);
            logService.addEntry(Severity.ERROR, "Detected checksum mismatch for uploaded file while processing Package: " + packageEntity.getName() + " | " + packageEntity.getUuid() + WITH_ERROR + e.getMessage());
            finish(packageEntity, plaintextFile, null);
            return;
        }

        Path encryptedFilePath = Paths.get(basePath + File.separator + packageEntity.getUuid());
        Path decryptedTestFilePath = Paths.get(basePath + File.separator + packageEntity.getUuid() + "_temp-encrypted-test");

        // encrypt file
        if (!cryptoUtility.encryptFile(packageEntity, plaintextFile, encryptedFilePath)) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR_ENCRYPTION);
            logService.addEntry(Severity.ERROR, "Failed to encrypt file while processing Package: " + packageEntity.getName() + " | " + packageEntity.getUuid());
            finish(packageEntity, plaintextFile, decryptedTestFilePath.toFile());
            return;
        }

        //calculate checksum of encrypted file
        String checksumEncryptedFile;
        try {
            checksumEncryptedFile = cryptoUtility.calculateChecksum(new FileInputStream(encryptedFilePath.toString()));
        } catch (Exception e) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
            logService.addEntry(Severity.ERROR, "Failed to calculate checksum of the encrypted file while processing Package: " + packageEntity.getName() + " | " + packageEntity.getUuid() + WITH_ERROR + e.getMessage());
            finish(packageEntity, plaintextFile, decryptedTestFilePath.toFile());
            return;
        }

        //decrypt file
        if (!cryptoUtility.decryptFile(packageEntity, new File(encryptedFilePath.toString()), decryptedTestFilePath)) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR_DECRYPTION);
            logService.addEntry(Severity.ERROR, "Failed to decrypt file while processing Package: " + packageEntity.getName() + " | " + packageEntity.getUuid());
            finish(packageEntity, plaintextFile, decryptedTestFilePath.toFile());
            return;
        }

        //compare checksum of decrypted file
        try {
            if (!cryptoUtility.calculateChecksum(new FileInputStream(decryptedTestFilePath.toString())).equals(packageEntity.getChecksumPlaintext())) {
                packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR_CHECKSUM_MISMATCH);
                logService.addEntry(Severity.ERROR, "Failed to calculate checksum of the decrypted file while processing Package: " + packageEntity.getName() + " | " + packageEntity.getUuid());
                finish(packageEntity, plaintextFile, decryptedTestFilePath.toFile());
                return;
            }
        } catch (FileNotFoundException e) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR_CHECKSUM_MISMATCH);
            logService.addEntry(Severity.ERROR, "Failed to calculate checksum of the encrypted file while processing Package: " + packageEntity.getName() + " | " + packageEntity.getUuid() + WITH_ERROR + e.getMessage());
            finish(packageEntity, plaintextFile, decryptedTestFilePath.toFile());
            return;
        }

        packageEntity.setChecksumEncrypted(checksumEncryptedFile);
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);

        packageEntity.setPlaintextSize(plaintextFile.length());
        packageEntity.setEncryptedSize(encryptedFilePath.toFile().length());

        finish(packageEntity, plaintextFile, decryptedTestFilePath.toFile());
        logService.addEntry(Severity.INFO, "Successfully processed Package: " + packageEntity.getName() + " | " + packageEntity.getUuid());
    }

    private void finish(PackageEntity packageEntity, File plaintextFile, File decryptedTestFile) {
        packageRepository.save(packageEntity);
        try {
            if (decryptedTestFile != null && !Files.deleteIfExists(decryptedTestFile.toPath())) {
                logService.addEntry(Severity.INFO, "Could not delete the decrypted test file for: " + packageEntity.getName() + " | " + packageEntity.getUuid());

            }
        } catch (Exception e) {
            logService.addEntry(Severity.INFO, "Could not delete the decrypted test file for: " + packageEntity.getName() + " | " + packageEntity.getUuid() + WITH_ERROR + e.getMessage());
        }
        try {
            if (!Files.deleteIfExists(plaintextFile.toPath())) {
                logService.addEntry(Severity.INFO, "Could not delete the plaintext file for: " + packageEntity.getName() + " | " + packageEntity.getUuid());
            }
        } catch (Exception e) {
            logService.addEntry(Severity.INFO, "Could not delete the plaintext file for: " + packageEntity.getName() + " | " + packageEntity.getUuid() + WITH_ERROR + e.getMessage());
        }
    }

}
