# Third-party licence texts

Texts that a third-party licence asks to be reproduced with a binary distribution, kept verbatim and packaged into the app
(Settings, Open-source licences) by the `collectLicenseAssets` task in `app/build.gradle.kts`. Every file here except this
one is shown in the app, so each must be listed in `LicenseDocuments`; `LicenseDocumentsTest` fails when they disagree.

The FFmpeg and AndroidX Media texts are not kept here: they travel inside the pinned FFmpeg bundle and are unpacked from it,
so they cannot drift from the libraries. Aptura's own `LICENSE` and `NOTICE` are read from the repository root.

| File | Source | SHA-256 | Why it is needed |
|---|---|---|---|
| `Protobuf-LICENSE-BSD-3-Clause.txt` | https://github.com/protocolbuffers/protobuf/blob/main/LICENSE (fetched 2026-11-14) | `6e5e117324afd944dcf67f36cf329843bc1a92229a8cd9bb573d7a83130fea7d` | `androidx.datastore:datastore-preferences-external-protobuf` repackages Protocol Buffers, which is BSD-3-Clause: binary distributions must reproduce its copyright notice, conditions and disclaimer. Found by reading the licences of the release dependency graph on 2026-11-14. |

To add a text: put it here, add a row above, list it in `LicenseDocuments`, and add the component to `NOTICE`.
