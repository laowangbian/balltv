# Network dependencies and root certificates

Conscrypt 2.5.2: https://github.com/google/conscrypt/tree/2.5.2 (Apache-2.0; accompanying NOTICE and BoringSSL license included).
AndroidX Multidex 2.0.1: official Google Maven artifact; Apache-2.0, POM and license included.

CA sources: https://letsencrypt.org/certs/isrgrootx1.pem , https://letsencrypt.org/certs/isrg-root-x2.pem , https://pki.goog/roots.pem . Only the following self-signed CA roots were retained. Certificates are public trust material, not private keys.

- C=US, O=Internet Security Research Group, CN=ISRG Root X1
  SHA256: 96:BC:EC:06:26:49:76:F3:74:60:77:9A:CF:28:C5:A7:CF:E8:A3:C0:AA:E1:1A:8F:FC:EE:05:C0:BD:DF:08:C6
- C=US, O=Internet Security Research Group, CN=ISRG Root X2
  SHA256: 69:72:9B:8E:15:A8:6E:FC:17:7A:57:AF:B7:17:1D:FC:64:AD:D2:8C:2F:CA:8C:F1:50:7E:34:45:3C:CB:14:70
- C=US, O=Google Trust Services LLC, CN=GTS Root R1
  SHA256: D9:47:43:2A:BD:E7:B7:FA:90:FC:2E:6B:59:10:1B:12:80:E0:E1:C7:E4:E4:0F:A3:C6:88:7F:FF:57:A7:F4:CF
- C=US, O=Google Trust Services LLC, CN=GTS Root R2
  SHA256: 8D:25:CD:97:22:9D:BF:70:35:6B:DA:4E:B3:CC:73:40:31:E2:4C:F0:0F:AF:CF:D3:2D:C7:6E:B5:84:1C:7E:A8
- C=US, O=Google Trust Services LLC, CN=GTS Root R3
  SHA256: 34:D8:A7:3E:E2:08:D9:BC:DB:0D:95:65:20:93:4B:4E:40:E6:94:82:59:6E:8B:6F:73:C8:42:6B:01:0A:6F:48
- C=US, O=Google Trust Services LLC, CN=GTS Root R4
  SHA256: 34:9D:FA:40:58:C5:E2:63:12:3B:39:8A:E7:95:57:3C:4E:13:13:C8:3F:E6:8F:93:55:6C:D5:E8:03:1B:3C:7D

conscrypt-android-2.5.2.aar SHA256: 42d18979caf53f5ef68548c76d4c98b41adb910a32ad9448133f9c5b20bd65a3

multidex-2.0.1.aar SHA256: 42dd32ff9f97f85771b82a20003a8d70f68ab7b4ba328964312ce0732693db09
