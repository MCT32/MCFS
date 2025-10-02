# Filesystem documentation

## Format region
Equivalent to FAT's boot sector, but with no code

*Table for 2 blocks per byte, more encoding types later*

| Offset | Size | Content                                                                         |
|--------|------|---------------------------------------------------------------------------------|
| 0x0    | 4    | Block encoding type<br>For 2bpb, always 0x00000003 (binary would be 0b00000001) |
| 0x4    | 1    | Chunk addressing format<br>0x01 for Z-order curve                               |
| 0x5    | 3    | Chunk position X                                                                |
| 0x8    | 3    | Chunk position Y                                                                |
| 0xb    | 1    | Y level from min world height (0 or -64) (0-127)                                |
| 0xc    | 4    | Number of clusters (32kb each)                                                  |

## File Allocation Table
List of 32-bit entries of the size listed in the Format Region

Entry values:
- 0x00000000: empty cluster
- 0x00000001-0xFFFFFFFE: Used cluster with next cluster address
- 0xFFFFFFFF: End of chain cluster

## Directory Table
Root directory table is right after the FAT

| Offset | Size | Content                                                                                                                  |
|--------|------|--------------------------------------------------------------------------------------------------------------------------|
| 0x00   | 32   | UTF-8 encoded file name + extension. Max 32 bytes including null terminator<br>0x00 = Empty entry<br>0xFF = End of table |
| 0x20   | 8    | 64-bit creation date Unix timestamp                                                                                      |
| 0x28   | 8    | 64-bit modification date Unix timestamp                                                                                  |
| 0x30   | 8    | 64-bit access date Unix timestamp                                                                                        |
| 0x38   | 4    | 32-bit starting cluster address                                                                                          |
| 0x3c   | 6    | 48-bit file size in bytes                                                                                                |
| 0x42   | 1    | File attribute bit field.<br>Bit 0: Directory flag<br>Bits 1-7: Reserved, leave as 0                                     |
| 0x43   | 60   | Padding to align to 0x80. Might be used later.                                                                           |