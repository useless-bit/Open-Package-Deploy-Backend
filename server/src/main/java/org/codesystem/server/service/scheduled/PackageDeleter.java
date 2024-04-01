package org.codesystem.server.service.scheduled;

import lombok.RequiredArgsConstructor;
import org.codesystem.server.ServerApplication;
import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.codesystem.server.repository.PackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PackageDeleter {
    private final PackageRepository packageRepository;
    private final Logger logger = LoggerFactory.getLogger(PackageDeleter.class);


    @Scheduled(timeUnit = TimeUnit.SECONDS, fixedDelay = 1)
    @Async("deletePackageTask")
    public void deletePackage() {
        PackageEntity packageEntity = packageRepository.findFirstByPackageStatusInternal(PackageStatusInternal.MARKED_AS_DELETED);
        if (packageEntity != null) {
            deletePackage(packageEntity);
        }
    }

    private void deletePackage(PackageEntity packageEntity) {
        Path basePath = Paths.get(ServerApplication.PACKAGE_LOCATION);
        File packageFile = new File(basePath + File.separator + packageEntity.getUuid());
        try {
            if (Files.deleteIfExists(packageFile.toPath())) {
                logger.info("Could not delete the file for: {} | {}", packageEntity.getName(), packageEntity.getUuid());
            }
        } catch (IOException e) {
            logger.info("Could not delete the file for: {} | {}. Error: {}", packageEntity.getName(), packageEntity.getUuid(), e.getMessage());

        }
        packageRepository.delete(packageEntity);
    }

}
