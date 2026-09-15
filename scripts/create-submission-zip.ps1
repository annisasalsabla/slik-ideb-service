# Script untuk membuat file ZIP bersih siap kirim ke email (di bawah 25 MB)

$projectDir = "D:\Projects\slik-ideb-service"
$zipPath = "D:\Projects\slik-ideb-service-submission.zip"

Write-Host "Membersihkan target/ dan file sementara..." -ForegroundColor Cyan
if (Test-Path "$projectDir\target") {
    Remove-Item -Recurse -Force "$projectDir\target"
}

# Hapus zip lama jika ada
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}

Write-Host "Membuat ZIP submission ke $zipPath..." -ForegroundColor Green
$excludeList = @(".git", ".vscode", ".idea", "target")

# Kompresi folder project tanpa .git dan target
Get-ChildItem -Path $projectDir -Exclude $excludeList | Compress-Archive -DestinationPath $zipPath -CompressionLevel Optimal

$zipSize = (Get-Item $zipPath).Length / 1MB
Write-Host "File ZIP berhasil dibuat: $zipPath" -ForegroundColor Green
Write-Host "Ukuran file ZIP: $([math]::Round($zipSize, 2)) MB" -ForegroundColor Yellow
if ($zipSize -lt 25) {
    Write-Host "Status: Aman untuk dilampirkan langsung di email (Ukuran < 25 MB)" -ForegroundColor Green
} else {
    Write-Host "Status: Sebaiknya upload ke Google Drive karena > 25 MB" -ForegroundColor Yellow
}
