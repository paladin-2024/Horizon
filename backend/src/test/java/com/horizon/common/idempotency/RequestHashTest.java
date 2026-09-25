package com.horizon.common.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

class RequestHashTest {

    private static MockMultipartHttpServletRequest upload(String path, MultipartFile file) {
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(path);
        request.setContentType("multipart/form-data");
        request.addFile(file);
        return request;
    }

    private static MultipartFile csv(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/csv", content.getBytes());
    }

    @Test
    void theSameFileOnTheSamePathHashesTheSame() {
        String a = RequestHash.of(upload("/api/v1/accounts/1/imports", csv("a.csv", "same bytes")), null);
        String b = RequestHash.of(upload("/api/v1/accounts/1/imports", csv("a.csv", "same bytes")), null);

        assertThat(a).isEqualTo(b).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void differentBytesOfTheSameLengthHashDifferently() {
        String a = RequestHash.of(upload("/api/v1/accounts/1/imports", csv("a.csv", "amount,-1000")), null);
        String b = RequestHash.of(upload("/api/v1/accounts/1/imports", csv("a.csv", "amount,-2000")), null);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void aDifferentFileNameHashesDifferently() {
        String a = RequestHash.of(upload("/api/v1/accounts/1/imports", csv("march.csv", "same")), null);
        String b = RequestHash.of(upload("/api/v1/accounts/1/imports", csv("april.csv", "same")), null);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void theAccountIdInThePathIsPartOfTheHash() {
        String a = RequestHash.of(upload("/api/v1/accounts/1/imports", csv("a.csv", "same")), null);
        String b = RequestHash.of(upload("/api/v1/accounts/2/imports", csv("a.csv", "same")), null);

        assertThat(a).isNotEqualTo(b);
    }

    /** Refuses {@code getBytes()}: hashing must read the file as a stream, 32 MiB never sits in memory. */
    private static final class StreamOnlyFile implements MultipartFile {

        private static final long SIZE = 32L * 1024 * 1024;

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return "big.csv";
        }

        @Override
        public String getContentType() {
            return "text/csv";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public long getSize() {
            return SIZE;
        }

        @Override
        public byte[] getBytes() {
            throw new AssertionError("the request hash must not buffer the whole file");
        }

        @Override
        public InputStream getInputStream() {
            return new InputStream() {
                private long remaining = SIZE;

                @Override
                public int read() {
                    return remaining-- > 0 ? 'a' : -1;
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    if (remaining <= 0) {
                        return -1;
                    }
                    int count = (int) Math.min(length, remaining);
                    Arrays.fill(buffer, offset, offset + count, (byte) 'a');
                    remaining -= count;
                    return count;
                }
            };
        }

        @Override
        public void transferTo(File destination) throws IOException, IllegalStateException {
            throw new AssertionError("the request hash must not copy the file");
        }
    }

    @Test
    void aLargeFileIsHashedWhileStreamingNeverBuffered() {
        String hash = RequestHash.of(upload("/api/v1/accounts/1/imports", new StreamOnlyFile()), null);

        assertThat(hash).matches("[0-9a-f]{64}");
    }
}
