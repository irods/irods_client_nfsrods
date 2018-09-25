@test "list contents: /mnt/nfsrods/home/rods" {
    run ls -l /mnt/nfsrods/home/rods
    [ "$status" -eq 0 ]
}

@test "create file: /mnt/nfsrods/home/rods/test_file.txt" {
    run echo "important data" > /mnt/nfsrods/home/rods/test_file.txt
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
