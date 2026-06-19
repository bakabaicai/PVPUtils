package com.pvp_utils.client;

public final class Version {
    // 文件名前缀：例如 PVPUtils-v1.2 里的 PVPUtils
    public static final String NAME = "PVPUtils";

    // 版本号：例如 PVPUtils-v1.2-alpha.1 里的 1.2
    public static final String VERSION = "1.3";

    // 版本类型：0 = 正式版，1 = alpha，2 = beta
    public static final int TYPE = 2;

    // 修订号：0 不显示修订号，例如 alpha；1 则显示为 alpha.1
    public static final int REVISION = 4;

    //显示部分debug功能，正式版记得关闭
    public static final boolean DEBUG = false;

    private Version() {}
}
