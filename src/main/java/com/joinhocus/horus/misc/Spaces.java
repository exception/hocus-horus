package com.joinhocus.horus.misc;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import io.javalin.http.UploadedFile;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

@UtilityClass
public class Spaces {

    private final Logger LOGGER = LoggerFactory.getLogger(Spaces.class);
    private final AWSCredentials CREDS = new BasicAWSCredentials("<redacted>", "<redacted>");
    private final AWSCredentialsProvider CRED_PROVIDER = new AWSStaticCredentialsProvider(CREDS);

    private final String EDGE_BASE = "<redacted>";

    private final AmazonS3 SPACE = AmazonS3ClientBuilder
            .standard()
            .withCredentials(CRED_PROVIDER)
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                    "sfo2.digitaloceanspaces.com",
                    "sfo2"
            ))
            .build();

    public String uploadImage(UploadedFile file, String location, String name) throws Exception {
        byte[] bytes = IOUtils.readBytes(file.getContent());
        long length = bytes.length;
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(length);
        meta.setContentType(file.getContentType());
        String path = "images/" + location + "/" + name + file.getExtension();

        // we do a new array here because it's already read from the stream
        PutObjectRequest request = new PutObjectRequest("hocus-media", path, new ByteArrayInputStream(bytes), meta)
                .withCannedAcl(CannedAccessControlList.PublicRead);
        SPACE.putObject(request);
        return EDGE_BASE + path;
    }

}
