package kr.inuappcenterportal.inuportal.global.service;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class ImageService {


    public void saveImageWithThumbnail(Long id, List<MultipartFile> images, String path) throws IOException {
        saveImage(id,images,path);
        saveThumbnail(images.get(0),path+"/thumbnail",id);
    }

    public void saveOnlyImage(String name, List<MultipartFile> images, String path) throws IOException {
        for (int i = 1; i < images.size() + 1; i++) {
            MultipartFile file = images.get(i - 1);
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getExtension(originalFilename);
            String fileName = name + fileExtension;
            Path filePath = Paths.get(path, fileName);
            Files.write(filePath, file.getBytes());
        }
    }
    public void saveImage(Long id, List<MultipartFile> images, String path) throws IOException {
        for (int i = 1; i < images.size() + 1; i++) {
            MultipartFile file = images.get(i - 1);
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getExtension(originalFilename);
            String fileName = id + "-" + i + fileExtension;
            Path filePath = Paths.get(path, fileName);
            Files.write(filePath, file.getBytes());
        }
    }
    private String getExtension(String filename) {
        // 파일 확장자 추출 (예: .jpg, .png)
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex != -1 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex);
        }
        return "";
    }

    private void saveThumbnail(MultipartFile image,String path, Long id) throws IOException {
        String fileExtension = getExtension(image.getOriginalFilename());
        String thumbnailName = id + fileExtension;
        Path thumbnailPath = Paths.get(path, thumbnailName);
        Thumbnails.of(image.getInputStream())
                .size(150, 150)
                .outputFormat(fileExtension.replace(".", ""))
                .toFile(thumbnailPath.toFile());
    }

    public byte[] getImage(Long id, Long imageId, String path){
        String fileName = id+"-"+imageId;
        try {
            File directory = new File(path);
            File[] matchingFiles = directory.listFiles((dir, name) -> name.startsWith(fileName));
            File file = matchingFiles[0];
            Path filePath = file.toPath();
            return Files.readAllBytes(filePath);
        }
        catch (Exception e){
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
    }

    public void deleteAllImage(Long id, Long imageCount,String path) throws IOException {
        for(int i = 1 ; i < imageCount + 1 ; i++){
            String fileName = id + "-" + i;
            Path filePath = Paths.get(path, fileName);
            Files.deleteIfExists(filePath);
        }
        Path filePath = Paths.get(path+"/thumbnail", id.toString());
        Files.deleteIfExists(filePath);
    }

    public void deleteImages(Long id, String path) {
        try (Stream<Path> paths = Files.list(Paths.get(path))) {
            for (Path filePath : paths.filter(filePath -> filePath.getFileName().toString().startsWith(id + "-"))
                    .toList()) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
        try (Stream<Path> paths = Files.list(Paths.get(path+"/thumbnail"))) {
            for (Path filePath : paths.filter(filePath -> filePath.getFileName().toString().startsWith(id.toString()))
                    .toList()) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
    }

    public void updateImages(Long id, List<MultipartFile> images,String path) throws IOException {
        deleteImages(id, path);
        if (images == null) images = new ArrayList<>();
        saveImageWithThumbnail(id, images, path);
    }



}
