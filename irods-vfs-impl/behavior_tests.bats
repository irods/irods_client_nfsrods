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
    local data_object='empty.txt'
    touch $data_object
    rm $data_object
}

@test "create/remove collection: /mnt/nfsrods/home/rods/test.d" {
    cd /mnt/nfsrods/home/rods
    local collection='test.d'
    mkdir $collection
    rmdir $collection
}

@test "write/read non-empty data object: /mnt/nfsrods/home/rods/data_object.txt" {
    cd /mnt/nfsrods/home/rods

    local data_object='data_object.txt'
    echo 'Hello, NFSRODS!' > $data_object

    run cat $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    rm $data_object
}

@test "truncate non-empty data object to 5 bytes: /mnt/nfsrods/home/rods/data_object.txt" {
    cd /mnt/nfsrods/home/rods

    local data_object='data_object.txt'
    echo 'Hello, NFSRODS!' > $data_object
    run cat $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    truncate -s5 $data_object

    run cat $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "Hello" ]

    rm $data_object
}

@test "rename data object: /mnt/nfsrods/home/rods/data_object.txt -> /mnt/nfsrods/home/rods/renamed.txt" {
    cd /mnt/nfsrods/home/rods

    local data_object='data_object.txt'
    echo 'Hello, NFSRODS!' > $data_object
    run cat $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    local new_name='renamed.txt'
    mv $data_object $new_name

    run ls $new_name
    [ "$status" -eq 0 ]
    [ "$output" = "$new_name" ]

    rm $new_name
}

@test "move data object into sub-collection: /mnt/nfsrods/home/rods/data_object.txt -> /mnt/nfsrods/home/rods/col.d/data_object.txt" {
    cd /mnt/nfsrods/home/rods

    local data_object='data_object.txt'
    touch $data_object

    local collection='col.d'
    mkdir $collection
    mv $data_object $collection

    run ls $collection/$data_object
    echo "output = $output"
    [ "$status" -eq 0 ]
    [ "$output" = "$collection/$data_object" ]

    rm $collection/$data_object
    rmdir $collection
}

@test "move collection into sibling collection: /mnt/nfsrods/home/rods/col_0.d -> /mnt/nfsrods/home/rods/col_1.d/col_0.d" {
    cd /mnt/nfsrods/home/rods

    local collection_0='col_0.d'
    local collection_1='col_1.d'

    mkdir $collection_0
    mkdir $collection_1
    mv $collection_0 $collection_1

    run ls $collection_1
    echo "output = $output"
    [ "$status" -eq 0 ]
    [ "$output" = "$collection_0" ]

    rmdir $collection_1/$collection_0
    rmdir $collection_1
}

