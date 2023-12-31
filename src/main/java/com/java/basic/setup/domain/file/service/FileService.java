package com.java.basic.setup.domain.file.service;

import com.java.basic.setup.domain.file.dto.FileVo;
import com.java.basic.setup.domain.file.entity.FileMetadata;
import com.java.basic.setup.domain.file.repository.FileRepository;
import com.java.basic.setup.domain.file.repository.FileRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    Logger logger = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final FileRepositorySupport fileSupport;

    public FileMetadata storeFile(FileVo file) throws IOException {

        logger.info("file name: "+file.getFiles().getName());

        MultipartFile multipartFile = file.getFiles();
        String fileName = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        LocalDateTime uploadDate = LocalDateTime.now();

        // 파일 저장 로직
        // 예를 들어, 파일을 "/uploads" 디렉토리에 저장하고자 한다면
        String uploadDir = "C:\\uploads"; // 실제 사용할 경로로 변경 필요
        Path uploadPath = Paths.get(uploadDir);

        logger.info("file upload path: "+ uploadPath.toString());

        //디렉터리 없으면 생성
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        //파일 복사해서 지정된 경로에 붙여넣기
        Path filePath = uploadPath.resolve(fileName);
        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        String downloadUrl = filePath.toUri().toString(); // 실제 URL 형식에 맞춰서 수정 필요

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(fileName);
        fileMetadata.setFileSize(fileSize);
        fileMetadata.setUploadDate(uploadDate);
        fileMetadata.setDownloadUrl(downloadUrl);

        return fileRepository.save(fileMetadata);
    }

    public FileMetadata downloadFileFromUrl(String urlString) throws IOException{
        URL url = new URL(urlString);
        Path tempDir = Files.createTempDirectory("downloaded_files");
        String fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
        Path filePath = tempDir.resolve(fileName);

        try (InputStream in = url.openStream()) {
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(fileName);
        fileMetadata.setFileSize(Files.size(filePath));
        fileMetadata.setUploadDate(LocalDateTime.now());
        fileMetadata.setDownloadUrl(urlString); // 원본 URL 사용

        return fileRepository.save(fileMetadata);
    }

    public Resource downloadFilePath(String filePathString) throws MalformedURLException, FileNotFoundException {
//        Path filePath = Paths.get("C:\\uploads").resolve(fileName).normalize();
        Path filePath = Paths.get(filePathString).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        String fileName = filePath.getFileName().toString();

        if(resource.exists()) {
            return resource;
        } else {
            throw new FileNotFoundException("File not found " + fileName);
        }
    }


    public List<FileMetadata> getFilesBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        return fileSupport.findFilesBetweenDates(startDate, endDate);
    }

}
