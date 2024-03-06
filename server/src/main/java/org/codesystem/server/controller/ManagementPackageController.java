package org.codesystem.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.codesystem.server.request.packages.AddNewPackageRequest;
import org.codesystem.server.request.packages.UpdatePackageContentRequest;
import org.codesystem.server.request.packages.UpdatePackageRequest;
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
    public ResponseEntity<ApiResponse> addNewPackage(@RequestPart AddNewPackageRequest addNewPackageRequest, @RequestPart MultipartFile multipartFile) {
        return managementPackageService.addNewNewPackage(addNewPackageRequest, multipartFile);
    }

    @PatchMapping("{packageUUID}")
    public ResponseEntity<ApiResponse> updatePackage(@RequestBody UpdatePackageRequest updatePackageRequest, @PathVariable String packageUUID) {
        return managementPackageService.updatePackage(updatePackageRequest, packageUUID);
    }

    @PatchMapping(value = "{packageUUID}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> updatePackageContent(@RequestPart UpdatePackageContentRequest updatePackageRequest, @RequestPart MultipartFile multipartFile, @PathVariable String packageUUID) {
        return managementPackageService.updatePackageContent(updatePackageRequest, multipartFile, packageUUID);
    }

    @DeleteMapping("{packageUUID}")
    public ResponseEntity<ApiResponse> deletePackage(@PathVariable String packageUUID) {
        return managementPackageService.deletePackage(packageUUID);
    }
}
