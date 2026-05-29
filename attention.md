# 开发注意事项

## Android 开发

1. **AndroidX 配置**：使用 `android.useAndroidX=true` 和 `android.enableJetifier=true` 来确保兼容性
2. **Kotlin compileOptions**：使用新的 `compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }` 代替旧的 `kotlinOptions`
3. **图标资源**：确保 drawable 资源不使用 `?attr/...` 这类 theme 引用，直接使用具体颜色值
4. **Kotlin combine()**：`combine` 函数最多支持 5 个流，更多流需要使用 zip 或多个 combine 组合
5. **电池优化请求**：使用 `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 可以直接请求忽略电池优化
6. **UsageStats 权限**：需要使用 AppOpsManager 来检查是否已授予使用情况访问权限
7. **AndroidX 兼容**：使用 `NotificationCompat` 等兼容库来确保版本兼容性
8. **AppOpsManager API**：使用 `unsafeCheckOpNoThrow`（API 29+）代替 `checkOpNoThrow` 来避免警告
9. **权限请求方式**：
   - 普通权限：使用 Accompanist Permissions 库进行系统弹窗请求
   - 特殊权限（忽略电池优化、使用情况访问等）：需要跳转到系统设置页面
   - 使用 `ActivityResultContracts.StartActivityForResult()` 来处理从设置页面返回的逻辑
10. **Accompanist Permissions**：需要添加 `accompanist-permissions` 依赖，并使用 `@OptIn(ExperimentalPermissionsApi::class)`

## Gradle 开发

1. **KSP 兼容性**：KSP 版本需要与 Kotlin 版本兼容，建议使用匹配的稳定版本组合
2. **内存配置**：适当配置 Gradle JVM 内存，避免内存溢出错误
3. **Accompanist 依赖**：需要在 libs.versions.toml 中添加 accompanist 版本和对应的库引用
