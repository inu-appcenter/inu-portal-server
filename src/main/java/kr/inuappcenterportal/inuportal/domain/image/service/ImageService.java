package kr.inuappcenterportal.inuportal.domain.image.service;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.MediaType;
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

    // 이미지 확장자 순서대로
    private static final List<String> EXTENSION_PRIORITY = List.of(".webp", ".png", ".jpg", ".jpeg", ".gif");

    /**
     * 이미지/썸네일 저장 메서드
     */
    public void saveImageWithThumbnail(Long id, List<MultipartFile> images, String path) throws IOException {
        saveImage(id, images, path);
        saveThumbnail(images.get(0), path + "/thumbnail", id);
    }

    /**
     * 채팅방 이미지 저장 메서드
     */
    public void saveChatImage(Long roomId, Long messageId, List<MultipartFile> images, String basePath) throws IOException {
        Path roomPath = Paths.get(basePath, roomId.toString());
        Path thumbnailPath = roomPath.resolve("thumbnail");

        log.info("[DEBUG] 채팅방 경로: {}", roomPath.toAbsolutePath());
        log.info("[DEBUG] 썸네일 경로: {}", thumbnailPath.toAbsolutePath());

        if (!Files.exists(roomPath)) {
            Files.createDirectories(roomPath);
            log.info("[DEBUG] 채팅방 디렉토리 생성 완료");
        }
        if (!Files.exists(thumbnailPath)) {
            Files.createDirectories(thumbnailPath);
            log.info("[DEBUG] 썸네일 디렉토리 생성 완료");
        }
        saveImage(messageId, images, roomPath.toString());
        saveThumbnail(images.get(0), thumbnailPath.toString(), messageId);
    }

    /**
     * 이미지 저장 메서드(게시글 등)
     */
    public void saveImage(Long id, List<MultipartFile> images, String path) throws IOException {
        for (int i = 1; i < images.size() + 1; i++) {
            MultipartFile file = images.get(i - 1);
            BufferedImage resizedImage = Thumbnails.of(file.getInputStream())
                    .size(1080, 1080)
                    .keepAspectRatio(true)
                    .asBufferedImage();
            String fileName = id + "-" + i + ".webp";
            File outputFile = new File(path, fileName);
            ImageIO.write(resizedImage, "webp", outputFile);
        }
    }

    private void saveThumbnail(MultipartFile image, String path, Long id) throws IOException {
        BufferedImage thumbnail = Thumbnails.of(image.getInputStream())
                .size(400, 400)
                .keepAspectRatio(true)
                .asBufferedImage();
        String thumbnailName = id + ".webp";
        File outputFile = new File(path, thumbnailName);
        ImageIO.write(thumbnail, "webp", outputFile);
    }

    public byte[] getImage(Long id, Long imageId, String path) {
        try {
            File file = findImageFile(id, imageId, path);
            return Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
    }

    
    /**
     * 저장된 파일의 확장자를 보고 맞는 MediaType 리턴해주는 메서드
     */
    public MediaType getImageContentType(Long id, Long imageId, String path) {
        try {
            File file = findImageFile(id, imageId, path);
            return resolveContentType(file.getName());
        } catch (Exception e) {
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
    }

    private File findImageFile(Long id, Long imageId, String path) {
        String fileName = id + "-" + imageId;
        File directory = new File(path);
        File[] matchingFiles = directory.listFiles((dir, name) -> name.startsWith(fileName));
        if (matchingFiles == null || matchingFiles.length == 0) {
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
        if (matchingFiles.length == 1) {
            return matchingFiles[0];
        }
        for (String extension : EXTENSION_PRIORITY) {
            for (File file : matchingFiles) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    return file;
                }
            }
        }
        return matchingFiles[0];
    }

    private MediaType resolveContentType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".webp")) {
            return MediaType.valueOf("image/webp");
        }
        if (lowerFileName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lowerFileName.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /**
     * 모든 이미지 삭제 메서드
     */
    public void deleteAllImage(Long id, Long imageCount, String path) throws IOException {
        for (int i = 1; i < imageCount + 1; i++) {
            String fileName = id + "-" + i;
            Path filePath = Paths.get(path, fileName);
            Files.deleteIfExists(filePath);
        }
        Path filePath = Paths.get(path + "/thumbnail", id.toString());
        Files.deleteIfExists(filePath);
    }

    /**
     * 이미지 삭제 메서드
     */
    public void deleteImages(Long id, String path) {
        try (Stream<Path> paths = Files.list(Paths.get(path))) {
            for (Path filePath : paths.filter(filePath -> filePath.getFileName().toString().startsWith(id + "-"))
                    .toList()) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.error("이미지가 없어서 삭제시 오류 발생", e);
        }
        try (Stream<Path> paths = Files.list(Paths.get(path + "/thumbnail"))) {
            for (Path filePath : paths.filter(filePath -> filePath.getFileName().toString().startsWith(id.toString()))
                    .toList()) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.error("썸네일이 없어서 삭제시 오류 발생", e);
        }
    }

    public void updateImages(Long id, List<MultipartFile> images, String path) throws IOException {
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

    public String saveChatRoomThumbnail(Long roomId, MultipartFile thumbnail, String basePath) throws IOException {
        Path path = Paths.get(basePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        BufferedImage thumbImage = Thumbnails.of(thumbnail.getInputStream())
                .size(400, 400)
                .keepAspectRatio(true)
                .asBufferedImage();
        String fileName = "chat-room-thumb-" + roomId + ".webp";
        File outputFile = new File(basePath, fileName);
        ImageIO.write(thumbImage, "webp", outputFile);
        return "/images/" + fileName;
    }
}
