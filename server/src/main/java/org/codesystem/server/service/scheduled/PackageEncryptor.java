package org.codesystem.server.service.scheduled;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.PackageRepository;
import org.codesystem.server.utility.CryptoUtility;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PackageEncryptor {
    private final PackageRepository packageRepository;
    private final CryptoUtility cryptoUtility;

    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 1)
    @Async("encryptPackageTask")
    public void encryptPackage() {
        PackageEntity packageEntity = packageRepository.findFirstByPackageStatusInternal(PackageStatusInternal.UPLOADED);
        if (packageEntity != null) {
            encryptPackage(packageEntity);
        }
    }

    private void encryptPackage(PackageEntity packageEntity) {
        //check if file exists
        String basePath = "/opt/OPD/Packages/";
        File plaintextFile = new File(basePath + packageEntity.getUuid() + "_plaintext");
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSING);
        packageEntity = packageRepository.save(packageEntity);
        if (!plaintextFile.exists() || !plaintextFile.isFile()) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
            packageRepository.save(packageEntity);
            return;
        }


        //compare checksum of plaintext file
        try (FileInputStream fileInputStream = new FileInputStream(plaintextFile)) {
            if (!cryptoUtility.calculateChecksum(fileInputStream).equals(packageEntity.getChecksumPlaintext())) {
                packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
                packageRepository.save(packageEntity);
                return;
            }
        } catch (Exception e) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
            packageRepository.save(packageEntity);
            return;
        }

        Path encryptedFilePath = Paths.get(basePath + packageEntity.getUuid());
        Path decryptedTestFilePath = Paths.get(basePath + packageEntity.getUuid() + "_temp-encrypted-test");

        //encrypt file
        if (!cryptoUtility.encryptFile(packageEntity, plaintextFile, encryptedFilePath)) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
            packageRepository.save(packageEntity);
            return;
        }

        //calculate checksum of encrypted file
        String checksumEncryptedFile;
        try {
            checksumEncryptedFile = cryptoUtility.calculateChecksum(new FileInputStream(encryptedFilePath.toString()));
        } catch (FileNotFoundException e) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
            packageRepository.save(packageEntity);
            return;
        }

        //decrypt file
        if (!cryptoUtility.decryptFile(packageEntity, new File(encryptedFilePath.toString()), decryptedTestFilePath)) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
            packageRepository.save(packageEntity);
            return;
        }

        //compare checksum of decrypted file
        try {
            if (!cryptoUtility.calculateChecksum(new FileInputStream(decryptedTestFilePath.toString())).equals(packageEntity.getChecksumPlaintext())) {
                packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
                packageRepository.save(packageEntity);
                return;
            }
        } catch (FileNotFoundException e) {
            packageEntity.setPackageStatusInternal(PackageStatusInternal.ERROR);
            packageRepository.save(packageEntity);
            return;
        }

        packageEntity.setChecksumEncrypted(checksumEncryptedFile);
        packageEntity.setPackageStatusInternal(PackageStatusInternal.PROCESSED);
        packageRepository.save(packageEntity);

        //cleanup
        new File(decryptedTestFilePath.toString()).delete();
        plaintextFile.delete();
    }

}
