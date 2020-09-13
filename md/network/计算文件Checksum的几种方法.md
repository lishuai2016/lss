


Checksum：总和检验码，校验和。

在数据处理和数据通信领域中，用于校验目的的一组数据项的和。

这些数据项可以是数字或在计算检验总和过程中看作数字的其它字符串。

通常是以十六进制为数制表示的形式。

【作用】就是用于检查文件完整性，检测文件是否被恶意篡改，比如文件传输（如插件、固件升级包等）场景使用。


`可以使用不同的算法（MD5、SHA-1，SHA-256以及SHA-512）来计算Checksum`

几种计算文件checksum的方法：

- 1、使用java.security.MessageDigest
- 2、使用org.apache.commons.codec.digest.DigestUtils
- 3、使用com.google.common.io.Files.hash




1、依赖

```xml
 <!-- https://mvnrepository.com/artifact/commons-codec/commons-codec -->
        <dependency>
            <groupId>commons-codec</groupId>
            <artifactId>commons-codec</artifactId>
            <version>1.10</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/com.google.guava/guava -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>19.0</version>
        </dependency>

```


2、几种计算md5的方式


```java
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

public class MD5ChecksumGenerator {


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        System.out.println(String.format("genMD5Checksum1() --> %s",
                MD5ChecksumGenerator.genMD5Checksum1(new File("d://apache-tomcat-8.5.39.zip"))));

        System.out.println(String.format("genMD5Checksum2() --> %s",
                MD5ChecksumGenerator.genMD5Checksum2(new File("d://apache-tomcat-8.5.39.zip"))));

        System.out.println(String.format("genMD5Checksum3() --> %s",
                MD5ChecksumGenerator.genMD5Checksum3(new File("d://apache-tomcat-8.5.39.zip"))));

        System.out.println(String.format("genMD5Checksum4() --> %s",
                MD5ChecksumGenerator.genMD5Checksum4(new File("d://apache-tomcat-8.5.39.zip"))));
    }


    public static String genMD5Checksum1(File file) throws NoSuchAlgorithmException, IOException {

        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(Files.readAllBytes(file.toPath()));
        byte[] digestBytes = messageDigest.digest();
        StringBuffer sb = new StringBuffer();
        for (byte b : digestBytes) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static String genMD5Checksum2(File file) throws NoSuchAlgorithmException, IOException {

        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(Files.readAllBytes(file.toPath()));
        byte[] digestBytes = messageDigest.digest();
        return DatatypeConverter.printHexBinary(digestBytes).toLowerCase();
    }

    public static String genMD5Checksum3(File file) throws FileNotFoundException, IOException {
        /**
         * 使用org.apache.commons.codec.digest.DigestUtils
         */
        String md5Checksum = DigestUtils.md5Hex(new FileInputStream(file));
        return md5Checksum;
    }

    public static String genMD5Checksum4(File file) throws IOException {
        /**
         * 使用Guava
         */
        HashCode md5Hash = com.google.common.io.Files.hash(file, Hashing.md5());
        return md5Hash.toString();
    }
}

```



输出

```
genMD5Checksum1() --> 0ac04571f1103affb711bb4263c8ac86
genMD5Checksum2() --> 0ac04571f1103affb711bb4263c8ac86
genMD5Checksum3() --> 0ac04571f1103affb711bb4263c8ac86
genMD5Checksum4() --> 0ac04571f1103affb711bb4263c8ac86

```


# 参考

- [文件使用MD5 CheckSum的目的？](https://my.oschina.net/wangmengjun/blog/898496)


