#
# NFSRODS Behavior Tests
# ~~~~~~~~~~~~~~~~~~~~~~
#
# Requirement: NFSRODS must be mounted at /mnt/nfsrods.
#

@test "list contents: /mnt/nfsrods/home/rods" {
    run ls -l /mnt/nfsrods/home/rods
    [ "$status" -eq 0 ]
}

@test "create file: /mnt/nfsrods/home/rods/test_file.txt" {
    run touch /mnt/nfsrods/home/rods/test_file.txt
    [ "$status" -eq 0 ]
}

@test "remove file: /mnt/nfsrods/home/rods/test_file.txt" {
    run rm /mnt/nfsrods/home/rods/test_file.txt
    [ "$status" -eq 0 ]
}

@test "create directory: /mnt/nfsrods/home/rods/test.d" {
    run mkdir /mnt/nfsrods/home/rods/test.d
    [ "$status" -eq 0 ]
}

@test "remove directory: /mnt/nfsrods/home/rods/test.d" {
    run rmdir /mnt/nfsrods/home/rods/test.d
    [ "$status" -eq 0 ]
}

@test "create non-empty file: echo 'Hello, NFSRODS!' > /mnt/nfsrods/home/rods/test_file.txt" {
    run $(echo 'Hello, NFSRODS!' > /mnt/nfsrods/home/rods/test_file.txt)
    [ "$status" -eq 0 ]
}

@test "read non-empty file: /mnt/nfsrods/home/rods/test_file.txt" {
    run cat /mnt/nfsrods/home/rods/test_file.txt
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]
}

@test "truncate non-empty file to 5 bytes: /mnt/nfsrods/home/rods/test_file.txt" {
    run $(truncate -s5 /mnt/nfsrods/home/rods/test_file.txt)
    [ "$status" -eq 0 ]

    run cat /mnt/nfsrods/home/rods/test_file.txt
    [ "$status" -eq 0 ]
    [ "$output" = "Hello" ]

    run rm /mnt/nfsrods/home/rods/test_file.txt
    [ "$status" -eq 0 ]
}
