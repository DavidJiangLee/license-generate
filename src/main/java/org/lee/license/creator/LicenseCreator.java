package org.lee.license.creator;

import de.schlichtherle.license.*;
import lombok.extern.slf4j.Slf4j;
import org.lee.license.manager.CustomLicenseManager;
import org.lee.license.param.CustomKeyStoreParam;
import org.lee.license.param.License;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.text.MessageFormat;
import java.util.prefs.Preferences;

/**
 * License生成类 -- 用于license生成
 */
@Slf4j
public class LicenseCreator {

    private static final X500Principal DEFAULT_HOLDER_AND_ISSUER = new X500Principal("CN=Lee, OU=Lee, O=Lee, L=Dalian, ST=Liaoning, C=zn");

    private License license;

    public LicenseCreator(License license) {
        this.license = license;
    }

    /**
     * 生成License证书
     */
    public boolean generateLicense() {
        try {
            LicenseManager licenseManager = new CustomLicenseManager(initLicenseParam());
            LicenseContent licenseContent = initLicenseContent();
            licenseManager.store(licenseContent, new File(license.getLicensePath()));
            return true;
        } catch (Exception e) {
            log.error(MessageFormat.format("证书生成失败：{0}", license), e);
            return false;
        }
    }

    /**
     * 初始化证书生成参数
     */
    private LicenseParam initLicenseParam() {
        Preferences preferences = Preferences.userNodeForPackage(LicenseCreator.class);

        //设置对证书内容加密的秘钥
        CipherParam cipherParam = new DefaultCipherParam(license.getStorePass());

        KeyStoreParam privateStoreParam = new CustomKeyStoreParam(LicenseCreator.class
                , license.getPrivateKeysStorePath()
                , license.getPrivateAlias()
                , license.getStorePass()
                , license.getKeyPass());

        return new DefaultLicenseParam(license.getSubject()
                , preferences
                , privateStoreParam
                , cipherParam);
    }

    /**
     * 设置证书生成正文信息
     */
    private LicenseContent initLicenseContent() {
        LicenseContent licenseContent = new LicenseContent();
        if (null != license.getHolderAndIssuer()) {
            licenseContent.setHolder(license.getHolderAndIssuer());
            licenseContent.setIssuer(license.getHolderAndIssuer());
        } else {
            licenseContent.setHolder(DEFAULT_HOLDER_AND_ISSUER);
            licenseContent.setIssuer(DEFAULT_HOLDER_AND_ISSUER);
        }
        licenseContent.setSubject(license.getSubject());
        licenseContent.setIssued(license.getIssuedTime());
        licenseContent.setNotBefore(license.getIssuedTime());
        licenseContent.setNotAfter(license.getExpiryTime());
        licenseContent.setConsumerType(license.getConsumerType());
        licenseContent.setConsumerAmount(license.getConsumerAmount());
        licenseContent.setInfo(license.getDescription());

        //扩展校验，这里可以自定义一些额外的校验信息(也可以用json字符串保存)
        if (license.getLicenseExtraModel() != null) {
            licenseContent.setExtra(license.getLicenseExtraModel());
        }

        return licenseContent;
    }

}

