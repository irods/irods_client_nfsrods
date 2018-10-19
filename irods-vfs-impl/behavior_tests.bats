#
# NFSRODS Behavior Tests
# ~~~~~~~~~~~~~~~~~~~~~~
#
# Requirements:
# - Kerberos principal for the iRODS service account (e.g. rods@REALM).
# - NFSRODS Docker test container.
# - NFSRODS must be mounted at /mnt/nfsrods.
#
# How to run the tests:
# $ docker run -d --rm --name irods -h irods_test irods_ub16_postgres
# $ Configure NFSRODS to point to the container.
# $ kinit rods
# $ sudo mount -o sec=krb5,port=2050 <hostname>:/ <mount_point>
# $ bats behaviour_tests.bat
#

@test "list contents" {
    ls /mnt/nfsrods/home/rods
}

@test "create/remove data object" {
    cd /mnt/nfsrods/home/rods
    local data_object='empty.txt'
    touch $data_object
    rm $data_object
}

@test "create/remove collection" {
    cd /mnt/nfsrods/home/rods
    local collection='test.d'
    mkdir $collection
    rmdir $collection
}

@test "write/read non-empty data object" {
    cd /mnt/nfsrods/home/rods

    local data_object='data_object.txt'
    echo 'Hello, NFSRODS!' > $data_object

    run cat $data_object
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    rm $data_object
}

@test "truncate non-empty data object to 5 bytes" {
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

@test "rename data object" {
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

@test "move data object into sub-collection" {
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

@test "move collection into sibling collection" {
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

