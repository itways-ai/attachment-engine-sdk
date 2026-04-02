# Attachment Engine SDK — Full Documentation

> **Type:** Internal Java SDK / Library
> **Group:** `com.itways.assistant` | **Artifact:** `attachment-engine-sdk` (Folder: `upload-engine-sdk`) | **Version:** `0.0.1`
> **Java:** 21 | **Spring Boot:** 3.2.2

---

## 1. High-Level Overview

`attachment-engine-sdk` is an embeddable file-storage utility for the AI Assistant Platform.

**What it does:**
It abstracts away the complexity of interacting with object storage systems (specifically Cloudflare R2). It provides a simple, clean interface for microservices to upload raw byte arrays and securely retrieve public URLs, or download hosted text content back into memory.

**Its role in the system:**
It prevents microservice databases from bloating. By offloading files (images, long HTML documents) to cheap object storage like R2, it keeps relational databases (PostgreSQL) fast and strictly metadata-focused.

**Who uses it:**
- **`account-service`**: Uses it to upload user avatar images.
- **`template-service`**: Uses it to save the heavy HTML codes of templates as external files, storing only the returned Cloudflare URL in its database.

---

## 2. Architecture / Structure

The SDK is built using Spring Boot's Auto-Configuration principles, ensuring that consuming services don't have to manually wire up AWS S3 clients.

### Key Components

- **`@EnableAttachment`**: A custom annotation that host services use to trigger the configuration.
- **`UploadAutoConfiguration`**: Boots up the `AmazonS3` client, injecting custom Cloudflare endpoint URLs bypassing the default AWS endpoints.
- **`CloudFlareR2Config`**: A strongly typed property holder for `cloudflare.r2.*` settings.
- **`AttachmentService`**: The core public interface.
- **`CloudflareAttachmentService`**: The concrete implementation that manages S3 Put requests and Java 11 `HttpClient` Get requests.

---

## 3. Public APIs / Methods

### `AttachmentService`

This is the primary gateway exposed to host applications.

```java
@Autowired
private AttachmentService attachmentService;
```

#### Method: `upload(String fileName, byte[] bytes)`
Uploads a file to Cloudflare R2 and returns metadata containing the public URL.
```java
// Example usage in an API Controller
byte[] imageBytes = multipartFile.getBytes();
UploadResponse response = attachmentService.upload(multipartFile.getOriginalFilename(), imageBytes);

System.out.println(response.getPublicUrl()); 
// "https://pub-mycdn.com/safe-filename.png"
```

#### Method: `get(String url)`
Takes a public URL and downloads its content entirely into memory as a String. (Typically used to retrieve HTML templates).
```java
String htmlContent = attachmentService.get("https://pub-mycdn.com/template-123.ftl");
```
*Note: This method uses the native `java.net.http.HttpClient` to perform a standard HTTP GET instead of utilizing the authenticated Amazon S3 SDK. This implies the target URL must be publicly readable.*

---

## 4. Dependencies & SDK Usage

### 📦 EXTERNAL DEPENDENCY: `aws-java-sdk-s3` (Amazon)
**Purpose/Usage:** 
Because Cloudflare R2 is fully S3-compatible, the SDK leverages the official, battle-tested Amazon S3 library (`v1.12.772`) to handle chunking, multipart uploads, and authentication signing.

**Where it is used:**
In `UploadAutoConfiguration` to build the `AmazonS3` client, and in `CloudflareAttachmentService` where `putObject()` actually executes the network upload.

### 📦 EXTERNAL DEPENDENCY: `spring-boot-starter-web`
**Purpose/Usage:**
Used primarily to have access to Spring's `MultipartFile` interface wrapper. The internal `FileUtils` class wraps raw byte arrays into a `ByteArrayMultipartFile` so the S3 client can reliably extract Content-Type and sizing metadata before uploading.

### 📦 INTERNAL MODULES: None
**Deliberate Isolation:** Just like `ai-engine-sdk`, this module avoids depending on `common-lib`. It throws `java.lang.Exception` directly rather than depending on `BusinessException`.

---

## 5. Usage Examples

### Auto-Configuration Example (Host App)

To start uploading files, a microservice just needs three things:

1. **Add to POM**:
```xml
<dependency>
    <groupId>com.itways.assistant</groupId>
    <artifactId>attachment-engine-sdk</artifactId>
    <version>0.0.1</version>
</dependency>
```

2. **Enable via Annotation**:
```java
@EnableAttachment // <--- Wires the S3 client and Service Beans
@SpringBootApplication
public class TemplateServiceApplication { ... }
```

3. **Provide Keys in `application.properties`**:
```properties
cloudflare.r2.account-id=your-cloudflare-account-hash
cloudflare.r2.access-key=secret-access-key
cloudflare.r2.secret-key=secret-access-token
cloudflare.r2.bucket=my-app-storage
cloudflare.r2.public-base-url=https://pub-cdn.myapp.com
```

---

## 6. Known Issues / Technical Debt

| # | Issue | Severity | Details |
|---|---|---|---|
| 1 | **Generic Exceptions** | ⚠️ Medium | `AttachmentService` methods throw generic `java.lang.Exception`. This forces host applications to blindly catch `Exception` rather than specifically catching custom exceptions (like `UploadFailedException` or `FileNotFoundException`). |
| 2 | **Public Read Assumption** | 💡 Low | The `get(String url)` method retrieves downloads via an unauthenticated `java.net.http.HttpClient` GET request. It assumes the bucket policy allows public reads on the files. If you ever use this SDK to store private/sensitive attachments, `get()` cannot be used to retrieve them. |
| 3 | **Blocking S3 PUT** | 💡 Low | `amazonS3.putObject` is a synchronous, blocking network call. If a host service (e.g., `account-service` uploading an avatar) executes this during an active HTTP request, the client connection blocks until Cloudflare acknowledges receipt. |

---

## 7. Quick Mental Model

Imagine `attachment-engine-sdk` as a **Valet Parking Service for files**.

- Your microservice (the Driver) arrives with a heavy box (a massive byte array). 
- You don't want to carry that box into the restaurant (your relational database) because it takes up too much room and slows everything down.
- You hand the box to the SDK (the Valet).
- The Valet drives the box to a massive external warehouse (`Cloudflare R2`), parks it securely, and hands you back a small paper ticket (the `publicUrl`).
- You take that tiny paper ticket into the restaurant and save it.
- When you need the box back later (e.g., retrieving an HTML template), you hand the ticket back to the Valet (`get(url)`), and they drive to the warehouse to fetch the contents for you.

---

## 8. TL;DR

`attachment-engine-sdk` is an internal utility that encapsulates the Amazon S3 SDK to seamlessly interact with Cloudflare R2 object storage. Activated via `@EnableAttachment`, it provides microservices with a simple interface (`AttachmentService`) to offload byte arrays (files, templates, images) to the cloud and retrieve their public URLs, protecting native SQL databases from heavy blob saturation.
