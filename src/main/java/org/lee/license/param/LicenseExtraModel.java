package org.lee.license.param;

import lombok.Data;

import java.util.List;

/**
 * 自定义需要校验的License参数，可以增加一些额外需要校验的参数，比如项目信息，ip地址信息等等，待完善
 */
@Data
public class LicenseExtraModel {

    // 这里可以添加一些往外的自定义信息，比如我们可以增加项目验证，客户电脑sn码的验证等等
    /**
     * 可被允许的IP地址
     */

    private List<String> ipAddress;

    /**
     * 可被允许的MAC地址
     */

    private List<String> macAddress;

    /**
     * 可被允许的CPU序列号
     */

    private String cpuSerial;

    /**
     * 可被允许的主板序列号
     */

    private String mainBoardSerial;

}

