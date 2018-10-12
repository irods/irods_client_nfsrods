#
# NFSRODS Behavior Tests
# ~~~~~~~~~~~~~~~~~~~~~~
#
# Requirement: NFSRODS must be mounted at /mnt/nfsrods.
#

@test "list contents: /mnt/nfsrods/home/rods" {
    ls /mnt/nfsrods/home/rods
}

@test "create/remove data object: /mnt/nfsrods/home/rods/empty.txt" {
    cd /mnt/nfsrods/home/rods
    data_object=empty.txt
    touch $data_object
    sleep 1
    rm $data_object
}

@test "create/remove collection: /mnt/nfsrods/home/rods/test.d" {
    cd /mnt/nfsrods/home/rods
    collection=test.d
    mkdir $collection
    sleep 1
    rmdir $collection
}

@test "create/remove empty data object: /mnt/nfsrods/home/rods/empty.txt" {
    cd /mnt/nfsrods/home/rods

    data_object=empty.txt
    touch $data_object
    sleep 1

    run ls $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "$data_object" ]

    rm $data_object
}

@test "write/read non-empty data object: /mnt/nfsrods/home/rods/test.txt" {
    cd /mnt/nfsrods/home/rods

    data_object=test.txt
    echo 'Hello, NFSRODS!' > $data_object
    sleep 1

    run cat $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    rm $data_object
}

@test "truncate non-empty data object to 5 bytes: /mnt/nfsrods/home/rods/test.txt" {
    cd /mnt/nfsrods/home/rods

    data_object=test.txt
    echo 'Hello, NFSRODS!' > $data_object
    sleep 1
    run cat $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    truncate -s5 $data_object
    sleep 1

    run cat $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "Hello" ]

    rm $data_object
}

@test "rename data object: /mnt/nfsrods/home/rods/test.txt -> /mnt/nfsrods/home/rods/renamed.txt" {
    cd /mnt/nfsrods/home/rods

    data_object=test.txt
    echo 'Hello, NFSRODS!' > $data_object
    sleep 1
    run cat $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    new_name=renamed.txt
    mv $data_object $new_name
    sleep 1

    run ls $new_name
    [ "$status" -eq 0 ]
    [ "$output" = "$new_name" ]

    rm $new_name
}

@test "move data object into sub-collection: /mnt/nfsrods/home/rods/test.txt -> /mnt/nfsrods/home/rods/col.d/test.txt" {
    cd /mnt/nfsrods/home/rods

    data_object=test.txt
    touch $data_object
    sleep 1

    collection=col.d
    mkdir $collection
    sleep 1
    mv $data_object $collection
    sleep 1

    run ls $collection/$data_object
    [ "$status" -eq 0 ]
    [ "$output" = "$collection/$data_object" ]

    rm $collection/$data_object
    rmdir $collection
}

@test "move collection into sibling collection: /mnt/nfsrods/home/rods/col_0.d -> /mnt/nfsrods/home/rods/col_1.d/col_0.d" {
    cd /mnt/nfsrods/home/rods

    collection_0=col_0.d
    collection_1=col_1.d

    mkdir $collection_0
    sleep 1
    mkdir $collection_1
    sleep 1
    mv $collection_0 $collection_1
    sleep 1

    run ls $collection_1
    [ "$status" -eq 0 ]
    [ "$output" = "$collection_0" ]

    rmdir $collection_1/$collection_0
    rmdir $collection_1
}

