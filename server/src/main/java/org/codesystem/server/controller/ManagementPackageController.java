package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.request.packages.PackageAddNewRequest;
import org.codesystem.server.request.packages.PackageUpdateContentRequest;
import org.codesystem.server.request.packages.PackageUpdateRequest;
import org.codesystem.server.response.general.ApiResponse;
import org.codesystem.server.service.packages.ManagementPackageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Management Packages")
@RestController
@RequestMapping("/api/package")
@RequiredArgsConstructor
public class ManagementPackageController {
    private final ManagementPackageService managementPackageService;

    @GetMapping()
    public ResponseEntity<ApiResponse> getAllPackages() {
        return managementPackageService.getAllPackages();
    }

    @GetMapping("{packageUUID}")
    public ResponseEntity<ApiResponse> gepPackage(@PathVariable String packageUUID) {
        return managementPackageService.getPackage(packageUUID);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Schema(type = "multipartfile")
    public ResponseEntity<ApiResponse> addNewPackage(@RequestPart PackageAddNewRequest packageAddNewRequest, @RequestPart MultipartFile multipartFile) {
        return managementPackageService.addNewNewPackage(packageAddNewRequest, multipartFile);
    }

    @PatchMapping("{packageUUID}")
    public ResponseEntity<ApiResponse> updatePackage(@RequestBody PackageUpdateRequest packageUpdateRequest, @PathVariable String packageUUID) {
        return managementPackageService.updatePackage(packageUpdateRequest, packageUUID);
    }

    @PatchMapping(value = "{packageUUID}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> updatePackageContent(@RequestPart PackageUpdateContentRequest packageUpdateContentRequest, @RequestPart MultipartFile multipartFile, @PathVariable String packageUUID) {
        return managementPackageService.updatePackageContent(packageUpdateContentRequest, multipartFile, packageUUID);
    }

    @DeleteMapping("{packageUUID}")
    public ResponseEntity<ApiResponse> deletePackage(@PathVariable String packageUUID) {
        return managementPackageService.deletePackage(packageUUID);
    }
}
