package kr.inuappcenterportal.inuportal.domain.image.service;

import kr.inuappcenterportal.inuportal.domain.image.repository.ImageRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class ImageService {

    private final ImageRepository imageRepository;

    public void saveImageWithThumbnail(Long id, List<MultipartFile> images, String path) throws IOException {
        saveImage(id,images,path);
        saveThumbnail(images.get(0),path+"/thumbnail",id);
    }

    public void saveChatImage(Long roomId, Long messageId, List<MultipartFile> images, String basePath) throws IOException {
        Path roomPath = Paths.get(basePath, roomId.toString());
        Path thumbnailPath = roomPath.resolve("thumbnail");

        log.info("[DEBUG] Room Path: {}", roomPath.toAbsolutePath());
        log.info("[DEBUG] Thumbnail Path: {}", thumbnailPath.toAbsolutePath());

        if (!Files.exists(roomPath)) {
            Files.createDirectories(roomPath);
            log.info("[DEBUG] Room Directory Created");
        }
        if (!Files.exists(thumbnailPath)) {
            Files.createDirectories(thumbnailPath);
            log.info("[DEBUG] Thumbnail Directory Created");
        }
        saveImage(messageId, images, roomPath.toString());
        saveThumbnail(images.get(0), thumbnailPath.toString(), messageId);
    }

    public void saveImage(Long id, List<MultipartFile> images, String path) throws IOException {
        for (int i = 1; i < images.size() + 1; i++) {
            MultipartFile file = images.get(i-1);
            BufferedImage resizedImage = Thumbnails.of(file.getInputStream())
                    .size(1080, 1080)
                    .keepAspectRatio(true)
                    .asBufferedImage();
            String fileName = id + "-" + i + ".webp";
            File outputFile = new File(path, fileName);
            ImageIO.write(resizedImage, "webp", outputFile);
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
        BufferedImage thumbnail = Thumbnails.of(image.getInputStream())
                .size(400, 400)
                .keepAspectRatio(true)
                .asBufferedImage();
        String thumbnailName = id + ".webp";
        File outputFile = new File(path, thumbnailName);
        ImageIO.write(thumbnail, "webp", outputFile);
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
        catch (Exception e) {
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
            log.error("이미지가 없어서 삭제시 오류 발생", e);
        }
        try (Stream<Path> paths = Files.list(Paths.get(path+"/thumbnail"))) {
            for (Path filePath : paths.filter(filePath -> filePath.getFileName().toString().startsWith(id.toString()))
                    .toList()) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.error("썸네일이 없어서 삭제시 오류 발생", e);
        }
    }

    public void updateImages(Long id, List<MultipartFile> images,String path) throws IOException {
        deleteImages(id, path);
        if (images == null) images = new ArrayList<>();
        saveImageWithThumbnail(id, images, path);
    }

    public void deleteChatRoomImages(Long roomId, String basePath) throws IOException {
        Path roomPath = Paths.get(basePath, roomId.toString());
        if (Files.exists(roomPath)) {
            try (Stream<Path> walk = Files.walk(roomPath)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }





}
