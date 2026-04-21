Get-Content .env |
  Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } |
  ForEach-Object {
    $key, $value = $_ -split '=', 2
    $value = $value.Trim().Trim('"')
    [System.Environment]::SetEnvironmentVariable($key.Trim(), $value, 'Process')
  }

[System.Environment]::SetEnvironmentVariable('DB_URL', 'jdbc:mysql://localhost:3306/wisecart_dev?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false', 'Process')
[System.Environment]::SetEnvironmentVariable('DB_USERNAME', 'root', 'Process')
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'wisecart', 'Process')

./mvnw spring-boot:run