package org.codesystem.server.repository;

import org.codesystem.server.entity.PackageEntity;
import org.codesystem.server.enums.packages.PackageStatusInternal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackageRepository extends JpaRepository<PackageEntity, String> {
    

    PackageEntity findFirstByPackageStatusInternal(PackageStatusInternal packageStatusInternal);

    PackageEntity findFirstByUuid(String packageUUID);
}
