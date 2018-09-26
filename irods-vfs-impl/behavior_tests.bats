#
# NFSRODS Behavior Tests
# ~~~~~~~~~~~~~~~~~~~~~~
#
# Requirement: NFSRODS must be mounted at /mnt/nfsrods.
#

@test "list contents: /mnt/nfsrods/home/rods" {
    ls /mnt/nfsrods/home/rods
}

@test "create/remove file: /mnt/nfsrods/home/rods/test_file.txt" {
    cd /mnt/nfsrods/home/rods
    touch test_file.txt
    rm test_file.txt
}

@test "create/remove directory: /mnt/nfsrods/home/rods/test.d" {
    cd /mnt/nfsrods/home/rods
    mkdir test.d
    rmdir test.d
}

@test "write/read non-empty file: /mnt/nfsrods/home/rods/test_file.txt" {
    cd /mnt/nfsrods/home/rods

    echo 'Hello, NFSRODS!' > test_file.txt

    run cat test_file.txt
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    rm test_file.txt
}

@test "truncate non-empty file to 5 bytes: /mnt/nfsrods/home/rods/test_file.txt" {
    cd /mnt/nfsrods/home/rods

    echo 'Hello, NFSRODS!' > test_file.txt
    run cat test_file.txt
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    truncate -s5 test_file.txt

    run cat test_file.txt
    [ "$status" -eq 0 ]
    [ "$output" = "Hello" ]

    rm test_file.txt
}

@test "rename file: /mnt/nfsrods/home/rods/test_file.txt -> /mnt/nfsrods/home/rods/renamed.txt" {
    cd /mnt/nfsrods/home/rods

    echo 'Hello, NFSRODS!' > test_file.txt
    run cat test_file.txt
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    mv test_file.txt renamed.txt

    run ls renamed.txt
    [ "$status" -eq 0 ]
    [ "$output" = "renamed.txt" ]

    rm renamed.txt
}
