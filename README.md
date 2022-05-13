## license-generate

#### 介绍

    使用TrueLicense 生成和验证License（服务器许可），及Springboot项目如何集成TrueLicense

### 二、原理

    首先需要生成密钥对，方法有很多，JDK中提供的KeyTool即可生成。 </br>
    授权者保留私钥，使用私钥对包含授权信息（如截止日期，MAC地址等）的license进行数字签名。</br>
    公钥交给使用者（放在验证的代码中使用），用于验证license是否符合使用条件。

### 三、使用Keytool命令生成密钥对

> Keytool 是一个Java 数据证书的管理工具 ,Keytool 将密钥（key）和证书（certificates）存在一个称为keystore的文件中 在keystore里，包含两种数据： 密钥实体（Key entity）——密钥（secret key）又或者是私钥和配对公钥（采用非对称加密） 可信任的证书实体（trusted certificate entries）——只包含公钥

### 四、生成密钥的流程

#### 1、首先要用KeyTool工具来生成私匙库：（-alias别名 –validity 3650表示10年有效）

> keytool -genkey -alias privatekey -keystore privateKeys.store -validity 3650 -keysize 1024

<font color="red">注意！！！默认的密码策略是6未数字与字母，如果不遵守会报错，我这里使用123456q；要使用1024长度的密钥，否则在使用truelicense的时候会报错</font>
![img.png](image/img.png)

#### 2、然后把私匙库内的证书导出到一个文件当中

> keytool -export -alias privatekey -file certfile.cer -keystore privateKeys.store

    会得到一个certfile.cer，此证书是为了给使用方创建公钥证书，再生产公钥后此证书就没有用了。

![img_1.png](image/img_1.png)

#### 3、然后再把这个证书文件导入到公匙库

> keytool -import -alias publiccert -file certfile.cer -keystore publicCerts.store

    公钥的密码设置为了lee2205,获取到公钥库，publicCerts.store

![img_2.png](image/img_2.png)

> privateKeys.keystore：私钥，这个我们自己留着，不能泄露给别人。</br>
> publicCerts.keystore：公钥，这个给客户用的。在我们程序里面就是用他配合license进行授权信息的校验的。</br>
> certfile.cer：这个文件没啥用，可以删掉。

### 五、生成许可文件

    首先修改 LicenseCreator 中的 DEFAULT_HOLDER_AND_ISSUER，
    当然如果你使用我提供的私钥就不用修改，如果不是请根据你自己生成的秘钥，生成参数，传入。
    CreatorTest 测试包中的类
    修改许可证书保存的路径；私钥库所在地址；
    修改holderAndIssuer，根据你自己生成私钥库的时候填写的内容。
    可以修改授权时间 Calendar.MONTH-月|Calendar.DAY-日|Calendar.YEAR-年

> // 证书存储地址</br>
> param.setLicensePath("E:\\license.lic");</br>
> // 私钥库所在地址</br>
> param.setPrivateKeysStorePath("F:\\source\\gitee\\license-creator\\src\\main\\resources\\privateKeys.store");</br>
> X500Principal holderAndIssuer = new X500Principal("CN=Lee, OU=Lee, O=Lee, L=Dalian, ST=Liaoning, C=zn");</br>
> // 授权3个月时间</br>
> expiryCalendar.add(Calendar.MONTH, 3);</br>

    执行 GenTest.generateLicense() 方法，能够生成许可文件。

![img.png](image/img_3.png)

    可以看到在E盘中已经生成了许可文件

![img.png](image/img_4.png)

### 六、将生成的许可文件及公钥配置给需要使用的文件项目中

    此处使用springboot项目进行集成，首先需要将此工程打包。

首先需要将此工程打包后，传入maven私库或者导入项目，我这里上传到私库。
![img.png](image/img_5.png)

<font color="red">需要注意的事项： 发现是在通过keytool生成密钥对的时候，公、私钥库的密码不一样（与私钥密码无关），设置为一样的以后，就可以了。</font>

#### 1、导入相关依赖

    将上传到私库的包，导入到新建立的项目中，同时导入truelicense相关的依赖如下

```xml

<dependencies>
    <dependency>
        <groupId>org.lee</groupId>
        <artifactId>license-generate</artifactId>
        <version>1.0.0</version>
    </dependency>

    <dependency>
        <groupId>de.schlichtherle.truelicense</groupId>
        <artifactId>truelicense-core</artifactId>
    </dependency>
</dependencies>
```

#### 2、在yaml文件中添加配置，文件路径及公钥库密码请根据你自己的实际情况配置

```yaml

license:
  subject: test-license
  public-alias: publicCert
  public-store-pass: 123456q
  license-path: E:\license\license.lic
  public-keys-store-path: E:\license\publicCerts.store
```

#### 3、 创建校验类及拦截器

    首先创建校验类，然后创建拦截器；通过拦截器校验器去调用校验方法，如果许可超过有效期则进行提示。

```java

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import de.schlichtherle.license.*;
import lombok.extern.slf4j.Slf4j;
import org.lee.license.manager.CustomLicenseManager;
import org.lee.license.param.CustomKeyStoreParam;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * License校验类
 */
@Slf4j
public class LicenseVerify {

    /**
     * 证书subject
     */
    private final String subject;
    /**
     * 公钥别称
     */
    private final String publicAlias;
    /**
     * 访问公钥库的密码
     */
    private final String storePass;
    /**
     * 证书生成路径
     */
    private final String licensePath;
    /**
     * 密钥库存储路径
     */
    private final String publicKeysStorePath;
    /**
     * LicenseManager
     */
    private LicenseManager licenseManager;
    /**
     * 标识证书是否安装成功
     */
    private boolean installSuccess;

    public LicenseVerify(String subject, String publicAlias, String storePass, String licensePath, String publicKeysStorePath) {
        this.subject = subject;
        this.publicAlias = publicAlias;
        this.storePass = storePass;
        this.licensePath = licensePath;
        this.publicKeysStorePath = publicKeysStorePath;
    }

    /**
     * 安装License证书，读取证书相关的信息, 在bean加入容器的时候自动调用
     */
    public void installLicense() {
        try {
            Preferences preferences = Preferences.userNodeForPackage(LicenseVerify.class);

            CipherParam cipherParam = new DefaultCipherParam(storePass);

            KeyStoreParam publicStoreParam = new CustomKeyStoreParam(LicenseVerify.class,
                    publicKeysStorePath,
                    publicAlias,
                    storePass,
                    null);
            LicenseParam licenseParam = new DefaultLicenseParam(subject, preferences, publicStoreParam, cipherParam);

            licenseManager = new CustomLicenseManager(licenseParam);
            licenseManager.uninstall();
            LicenseContent licenseContent = licenseManager.install(new File(licensePath));
            installSuccess = true;
            log.error("------------------------------- 证书安装成功 -------------------------------");
            log.error("证书有效期：{} - {}", DateUtil.format(licenseContent.getNotBefore(), DatePattern.NORM_DATETIME_PATTERN), DateUtil.format(licenseContent.getNotAfter(), DatePattern.NORM_DATETIME_PATTERN));
        } catch (Exception e) {
            installSuccess = false;
            e.printStackTrace();
            log.error("------------------------------- 证书安装成功 -------------------------------");
            log.error("证书已经超期");
        }
    }

    /**
     * 卸载证书，在bean从容器移除的时候自动调用
     */
    public void unInstallLicense() {
        if (installSuccess) {
            try {
                licenseManager.uninstall();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 校验License证书
     */
    public boolean verify() {
        try {
            licenseManager.verify();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
```

```java

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author ltb
 * @date 2022/5/11
 */
@Component
public class ValidateTimeInterceptor implements HandlerInterceptor {

    @Resource
    private LicenseVerify licenseVerify;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!licenseVerify.verify()) {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            JSONObject retData = new JSONObject();
            retData.put("code", 500);
            retData.put("msg", "您的证书无效，请核查服务器是否取得授权或重新申请证书！");
            response.getWriter().write(retData.toJSONString());
            return false;
        }
        return true;
    }
}

```

#### 4、创建配置类，配置方法能够安装上许可证书

```java


import com.hstech.license.LicenseVerify;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LicenseConfig {

    /**
     * 证书subject
     */
    @Value("${license.subject}")
    private String subject;

    /**
     * 公钥别称
     */
    @Value("${license.public-alias}")
    private String publicAlias;

    /**
     * 访问公钥库的密码
     */
    @Value("${license.public-store-pass}")
    private String storePass;

    /**
     * 证书生成路径
     */
    @Value("${license.license-path}")
    private String licensePath;

    /**
     * 密钥库存储路径
     */
    @Value("${license.public-keys-store-path}")
    private String publicKeysStorePath;

    @Bean(initMethod = "installLicense", destroyMethod = "unInstallLicense")
    public LicenseVerify licenseVerify() {
        return new LicenseVerify(subject, publicAlias, storePass, licensePath, publicKeysStorePath);
    }

}
```

#### 5、启动项目

    可以在控制台上看到，有效期的起始时间

![img.png](image/img_6.png)

    至此，对于使用truelicense的许可证书的使用就介绍完成了，都看到这里了，麻烦给个小星星，您的每个小星星都是对我的支持。