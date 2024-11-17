package com.vstream.video_service.service;

import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileService {
    public String getFileName(MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            return file.getOriginalFilename();
        }
        throw new IllegalArgumentException("File is null or empty.");
    }

    // Method to get file size
    public long getFileSize(MultipartFile file) {
        return file.getSize(); // Returns file size in bytes
    }

    public String getVideoDuration(Path videoFilePath) throws Exception {
        String duration = "";

        String command = "ffmpeg -i " + videoFilePath.toAbsolutePath();

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", command);
        processBuilder.redirectErrorStream(true); // Redirect error stream to capture output

        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Duration")) {
                    duration = line.substring(line.indexOf("Duration:") + 10, line.indexOf(", start")).trim();
                    break;
                }
            }
        }
        process.waitFor(); // Wait for the process to complete

        return duration;
    }

}
