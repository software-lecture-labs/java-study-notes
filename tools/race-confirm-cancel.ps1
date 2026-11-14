param(
    [string]$BaseUrl = "http://localhost:8080/sales/api/orders",
    [int]$Orders = 30
)

$body = '{"customerId":"VINET","lines":[{"productId":11,"unitPrice":14.00,"quantity":1}]}'
$ids = 1..$Orders | ForEach-Object {
    (Invoke-RestMethod -Method Post -Uri $BaseUrl -ContentType "application/json" -Body $body).orderId
}

$requests = $ids | ForEach-Object { "$_/confirm"; "$_/cancel" }
$results = $requests | ForEach-Object -Parallel {
    $response = Invoke-WebRequest -Method Post -Uri "$using:BaseUrl/$_" -SkipHttpErrorCheck
    $code = if ([int]$response.StatusCode -ge 400) { ([System.Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray()) | ConvertFrom-Json).code } else { "" }
    [pscustomobject]@{
        OrderId = $_.Split('/')[0]
        Action  = $_.Split('/')[1]
        Status  = [int]$response.StatusCode
        Code    = $code
    }
} -ThrottleLimit 60

"Responses:"
$results | Group-Object Status, Code | Select-Object Count, Name | Format-Table -AutoSize

$bothSucceeded = @($results | Where-Object Status -eq 200 | Group-Object OrderId | Where-Object Count -eq 2 | ForEach-Object Name)
"Orders where confirm and cancel both answered 200: $($bothSucceeded.Count)"
"Their final status:"
$bothSucceeded | ForEach-Object { Invoke-RestMethod "$BaseUrl/$_" } | Group-Object status | Select-Object Count, Name | Format-Table -AutoSize
