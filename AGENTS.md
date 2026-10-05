# 项目协作规则

## 注释

除非用户明确要求，新增或修改代码时禁止添加任何语言的注释，包括行注释、块注释、文档注释和内嵌注释。不要主动为代码补充解释性注释。本规则不要求清除已有注释。

## Minecraft API 核实

开始编码前必须阅读当前项目对应版本和映射的 Minecraft 源码，核实涉及的类、方法签名、字段及调用行为。禁止依赖训练知识库猜测 API。当前项目使用 Minecraft 1.21.11、官方 Mojang 映射和 Java 21。

源码未在 IDE 中显示时，先使用以下 PowerShell 命令列出本地源码归档：

```powershell
$project = 'E:\JavaCode\其他项目\PVPUtils-1.21.11'
$roots = @(
    (Join-Path $project '.gradle\loom-cache\minecraftMaven'),
    (Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom')
)
foreach ($root in $roots) {
    if (Test-Path -LiteralPath $root) {
        Get-ChildItem -LiteralPath $root -Recurse -File -Filter '*sources.jar' |
            Where-Object { $_.Name -like 'minecraft-*' } |
            Select-Object -ExpandProperty FullName
    }
}
```

选择与当前版本及映射匹配的源码 JAR；客户端与公共代码可能分处不同归档。使用 `jar tf` 列出归档内容：

```powershell
jar tf '找到的源码 JAR 绝对路径'
```

直接通过 ZIP 读取目标类源码，不要将反编译产物解压到项目根目录：

```powershell
$archive = '找到的源码 JAR 绝对路径'
$entryName = 'net/minecraft/client/gui/GuiGraphics.java'
$zip = [System.IO.Compression.ZipFile]::OpenRead($archive)
try {
    $entry = $zip.GetEntry($entryName)
    if ($null -eq $entry) { throw "源码条目不存在：$entryName" }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try { $reader.ReadToEnd() } finally { $reader.Dispose() }
} finally {
    $zip.Dispose()
}
```

若对应源码归档也已消失，先执行 `gradlew.bat genSources` 生成，再重新查找和阅读。命令应从项目根目录执行。针对客户端源码验证，应执行包含 `compileClientJava` 的编译任务，而非仅验证 `compileJava`。

## 构建环境警告（供后续 ChatGPT / Codex 阅读）

用户已指出当前 C 盘临时目录存在问题，会导致构建报错。当前观察到 `TEMP` 和 `TMP` 均为 `C:\Users\BAKA_B~1\AppData\Local\Temp`。此前运行 Gradle 在进入源码编译前报出 `java.io.IOException: Unable to establish loopback connection`。临时目录问题是用户提供的环境信息，尚未通过对照测试证明它是该 loopback 错误的唯一根因。

编译前先将当前进程的 `TEMP`、`TMP` 和 JVM `java.io.tmpdir` 指向项目内可写临时目录，避免继续使用上述 C 盘临时目录。保留现有 JVM 参数，仅追加临时目录设置，不要修改系统级环境变量：

```powershell
$project = 'E:\JavaCode\其他项目\PVPUtils-1.21.11'
$temp = Join-Path $project '.gradle\agent-tmp'
New-Item -ItemType Directory -Path $temp -Force | Out-Null
$env:TEMP = $temp
$env:TMP = $temp
$env:JAVA_TOOL_OPTIONS = (($env:JAVA_TOOL_OPTIONS + ' "-Djava.io.tmpdir=' + $temp + '"').Trim())
Set-Location -LiteralPath $project
.\gradlew.bat compileJava compileClientJava --no-daemon
```

若仍有 loopback 报错，进一步检查本机回环连接、Java 进程及环境限制；不要将编译前的环境失败误报为源码编译错误。生成源码同样遵循这项临时目录设置。

## 本地文件

IRC 已从当前分支代码移除，不要恢复本地旧 IRC 源码、编译产物或运行数据。项目根目录的 `com` 和 `net` 属于 MC 反编译/提取产物目录，应保持排除。清理未跟踪文件时保留用户明确要求新建的文件和当前任务产物。
