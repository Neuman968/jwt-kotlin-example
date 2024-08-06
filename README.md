# Generating keypair


## Generating Private Key

Required step to generate an initial keypair for use in this project. Example command Using openssl.

Note: Replace prime256v1 with whatever elliptic curve algo you want. 

```bash
openssl ecparam -name prime256v1 -genkey -noout -out testkey.key
```

## Extracting Pub Key (Optional)

Extract the public key for use elsewhere.

```bash
openssl ec -in testkey.key -pubout -out testkey.pub
```


## Using the API

After you generate the testey file, you can interact with this in a number of ways.

### Viewing QR Code in browser
After generating the testkey.key file, you can open your browser and navigate to `http://localhost:8080/qr` and view 
the QR code as an image. Refreshing the page will give you a new QR + token.

### Getting the token as a string.

```bash
curl http://localhost:8080/token
```

Will return the JWT token raw as a string.

### Test Verifying the token.

```bash
curl http://localhost:8080/verify -d '<token>'
```

Will verify the signature and expiration time encoded in the token. Paste the token resulting from the `/token` endpoint
to verify the tokens signature and expiration.

The server will respond with one of 3 different statuses.

    VALID: The signature and Time are both valid.

    NOT_VALID: The signature is invalid.

    EXPIRED: The signature is valid, but the token has expired.