# Upload Engine SDK

A Spring Boot SDK for seamless file uploads to Cloudflare R2 storage. This library provides auto-configuration and a simple service interface for integrating cloud storage capabilities into your Spring Boot applications.

## Features

- 🚀 **Auto-configuration** - Zero boilerplate setup with Spring Boot auto-configuration
- ☁️ **Cloudflare R2 Support** - Built-in integration with Cloudflare R2 (S3-compatible)
- 🔧 **Simple API** - Clean service interface for file uploads
- 📦 **Spring Boot 3.5+** - Built on the latest Spring Boot framework
- 🔐 **Secure** - Configurable access credentials and bucket management
- 🎯 **Folder Organization** - Support for organizing uploads into folders

## Requirements

- Java 21+
- Spring Boot 3.5+
- Maven 3.6+

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.itways.assistant</groupId>
    <artifactId>attachment-engine-sdk</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Quick Start

### 1. Enable Upload SDK

Add the `@EnableUpload` annotation to your Spring Boot application:

### 2. Configure Cloudflare R2

Add the following properties to your `application.yml` or `application.properties`:

**application.yml:**
```yaml
cloudflare:
  r2:
    access-key: your-access-key
    secret-key: your-secret-key
    account-id: your-account-id
    bucket: your-bucket-name
    public-base-url: https://your-public-domain.com  # Optional: Full base URL
    public-domain: your-public-domain.com            # Optional: Domain only
```

**application.properties:**
```properties
# Cloudflare R2
cloudflare.r2.access-key=${CLOUDFLARE_R2_ACCESS_KEY:64a352a6e3befc5811990a2127c1db20}
cloudflare.r2.secret-key=${CLOUDFLARE_R2_SECRET_KEY:78a8c57fbbdf2fd0f0780d61ff60bcc2b6a6ae89d9b008f1b2bf27329dc47c5d}
cloudflare.r2.bucket=${CLOUDFLARE_R2_BUCKET:media-service}
cloudflare.r2.account-id=${CLOUDFLARE_R2_ACCOUNT_ID:78d23ccb3da0c2429824c8c4a3423f6d}
cloudflare.r2.public-domain=${CLOUDFLARE_R2_PUBLIC_DOMAIN:https://profily.site}
cloudflare.r2.public-base-url=${CLOUDFLARE_R2_PUBLIC_URL:https://profily.site}
```

> **Note:** Either `public-base-url` or `public-domain` must be configured. The SDK prefers `public-base-url` if both are provided.

### 3. Use the Upload Service

Inject the `UploadService` and start uploading files:

```java
import com.itways.assistant.upload_engine_sdk.service.UploadService;
import com.itways.assistant.upload_engine_sdk.dto.UploadResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final UploadService uploadService;

    public FileController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/upload")
    public UploadResponse uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) {
        return uploadService.upload(file, folder);
    }
}
```

## API Reference

### UploadService Interface

```java
public interface UploadService {
    UploadResponse upload(MultipartFile file, String folder);
}
```

**Parameters:**
- `file` - The multipart file to upload
- `folder` - The folder path within the bucket (e.g., "images", "documents/pdfs")

**Returns:** `UploadResponse` object containing:
- `fileName` - The full path of the uploaded file
- `url` - The public URL to access the file
- `success` - Boolean indicating upload success
- `message` - Success or error message

### UploadResponse DTO

```java
public class UploadResponse {
    private String fileName;
    private String url;
    private boolean success;
    private String message;
}
```

## Configuration Properties

| Property | Required | Description | Example |
|----------|----------|-------------|---------|
| `cloudflare.r2.access-key` | Yes | Cloudflare R2 access key | `abc123...` |
| `cloudflare.r2.secret-key` | Yes | Cloudflare R2 secret key | `xyz789...` |
| `cloudflare.r2.account-id` | Yes | Cloudflare account ID | `1234567890abcdef` |
| `cloudflare.r2.bucket` | Yes | R2 bucket name | `my-app-storage` |
| `cloudflare.r2.public-base-url` | No* | Full public URL base | `https://cdn.example.com` |
| `cloudflare.r2.public-domain` | No* | Public domain only | `cdn.example.com` |

*At least one of `public-base-url` or `public-domain` must be configured.

## How It Works

1. **Auto-Configuration**: The SDK uses Spring Boot's auto-configuration mechanism to automatically set up the necessary beans when `@EnableUpload` is present.

2. **S3 Compatibility**: Cloudflare R2 is S3-compatible, so the SDK uses the AWS S3 SDK under the hood.

3. **File Upload Flow**:
   - Receives a `MultipartFile` from your controller
   - Constructs the file path using the provided folder
   - Uploads to Cloudflare R2 with proper metadata
   - Returns a public URL for accessing the file

## Example Use Cases

### Upload User Profile Pictures

```java
@PostMapping("/profile/avatar")
public UploadResponse uploadAvatar(@RequestParam("avatar") MultipartFile file) {
    return uploadService.upload(file, "profiles/avatars");
}
```

### Upload Documents with Validation

```java
@PostMapping("/documents")
public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
    // Validate file
    if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("File is empty");
    }
    
    if (file.getSize() > 10_000_000) { // 10MB limit
        return ResponseEntity.badRequest().body("File too large");
    }
    
    // Upload
    UploadResponse response = uploadService.upload(file, "documents");
    
    if (response.isSuccess()) {
        return ResponseEntity.ok(response);
    } else {
        return ResponseEntity.status(500).body(response);
    }
}
```

### Organize Uploads by Date

```java
@PostMapping("/media")
public UploadResponse uploadMedia(@RequestParam("file") MultipartFile file) {
    String folder = "media/" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    return uploadService.upload(file, folder);
}
```

## Dependencies

The SDK includes the following key dependencies:

- **Spring Boot Starter Web** (3.5.10) - Web framework support
- **AWS Java SDK S3** (1.12.772) - S3-compatible storage client
- **Lombok** - Reduces boilerplate code

## Building from Source

```bash
# Clone the repository
git clone <repository-url>

# Navigate to the project
cd attachment-engine-sdk

# Build with Maven
./mvnw clean install

# Skip tests (if needed)
./mvnw clean install -DskipTests
```

## Troubleshooting

### "No publicBaseUrl or publicDomain configured"

**Solution:** Ensure you've configured either `cloudflare.r2.public-base-url` or `cloudflare.r2.public-domain` in your application properties.

### "Upload failed: Access Denied"

**Solution:** Verify your Cloudflare R2 credentials and ensure the bucket exists with proper permissions.

### Auto-configuration not working

**Solution:** Make sure you've added `@EnableUpload` to your main application class and the SDK is on your classpath.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is part of the IT Ways Assistant platform.

## Support

For issues, questions, or contributions, please contact the development team or open an issue in the repository.

---

**Package:** `com.itways.assistant.upload_engine_sdk`  
**Version:** 0.0.1-SNAPSHOT  
**Spring Boot:** 3.5.10  
**Java:** 21+