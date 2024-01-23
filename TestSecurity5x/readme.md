# oatu2 四种模式 

## 授权码模式 
http://127.0.0.1:8883/oauth/authorize?client_id=client&response_type=code&redirect_uri=http://www.baidu.com

curl --location 'http://client:secret@127.0.0.1:8883/oauth/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--header 'Cookie: JSESSIONID=E1ACAA7C11D1835427B6FB459F1F9039' \
--data-urlencode 'grant_type=authorization_code' \
--data-urlencode 'code=t5gueh' \
--data-urlencode 'redirect_uri=http://www.baidu.com'