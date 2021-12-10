#! /bin/bash

#
# NFSRODS BATS Test Suite
#
#   - https://github.com/bats-core/bats-core
#
# Requirements:
#
#   - iRODS v4.2.9+
#   - BATS
#   - GNU Parallel
#
# From within an NFSRODS mount point where you have full permission, meaning
# your iRODS home collection or a sub-collection, run the following command:
#
#   $ bats test_nfsrods.bats
#

LS_EXECUTABLE=/bin/ls
SANDBOX=${PWD}/nfsrods_testing_sandbox

setup() {
    mkdir -p ${SANDBOX}
    cd ${SANDBOX}
}

teardown() {
    cd ..
    rm -rf ${SANDBOX}
}

@test "create, list, rename, copy, move, and remove file" {
    local FILENAME=removeme

    # Create
    run echo ${FILENAME} > ${FILENAME}
    [ $status -eq 0 ]

    # List
    run ${LS_EXECUTABLE} ${FILENAME}
    [ $status -eq 0 ]
    [ "${output}" = "${FILENAME}" ]

    # Rename
    local NEW_FILENAME=removeme.renamed
    run mv ${FILENAME} ${NEW_FILENAME}
    [ $status -eq 0 ]

    # Copy
    local DIRECTORY=col.d
    mkdir ${DIRECTORY}
    run cp ${NEW_FILENAME} ${DIRECTORY}/${FILENAME}
    [ $status -eq 0 ]

    # Move
    run mv ${DIRECTORY}/${FILENAME} .
    [ $status -eq 0 ]

    # Remove
    run rm ${FILENAME}
    [ $status -eq 0 ]

    run ${LS_EXECUTABLE} ${FILENAME}
    [ $status -eq 2 ]
    [[ "${output}" == *"ls: cannot access"* ]]
    [[ "${output}" == *"${FILENAME}"* ]]
    [[ "${output}" == *"No such file or directory"* ]]
}

@test "create, rename, copy, move, and remove directory" {
    local DIRECTORY_0=col.d
    local DIRECTORY_1=col.d.renamed

    # Create
    mkdir ${DIRECTORY_0}

    # Rename
    mv ${DIRECTORY_0} ${DIRECTORY_1}

    # Copy and Move
    cp -a ${DIRECTORY_1} ${DIRECTORY_0}
    mv ${DIRECTORY_1} ${DIRECTORY_0}/${DIRECTORY_1}

    # Remove variants
    rmdir ${DIRECTORY_0}/${DIRECTORY_1}
    rm -rf ${DIRECTORY_0}
}

@test "listing directory with large number of entries does not trigger duplicate cookie error" {
    run parallel mkdir -p ::: c{001..125}
    [ $status -eq 0 ]

    run ${LS_EXECUTABLE}
    [[ ! $(dmesg | tail | grep 'has duplicate cookie') ]]
}

@test "listing directory with large number of entries prints all entries" {
    run parallel touch ::: foo{0001..6000}

    result="$(${LS_EXECUTABLE} | wc -l)"
    [ "$result" -eq 6000 ]

    run parallel rm ::: foo{0001..6000}
}

@test "create and remove directory" {
    local DIRECTORY=col.d
    mkdir ${DIRECTORY}
    rmdir ${DIRECTORY}
}

@test "write and read non-empty file" {
    local FILENAME=bar

    echo -n 'Hello, NFSRODS!' > ${FILENAME}

    run cat ${FILENAME}
    [ "$status" -eq 0 ]
    [ "$output" = "Hello, NFSRODS!" ]

    run rm ${FILENAME}
    [ $status -eq 0 ]
}

@test "rename file" {
    local FILENAME=foo
    touch ${FILENAME}
    run ${LS_EXECUTABLE} ${FILENAME}
    [ "$status" -eq 0 ]

    local NEW_FILENAME=renamed.txt
    mv ${FILENAME} ${NEW_FILENAME}

    run ${LS_EXECUTABLE} ${NEW_FILENAME}
    [ "$status" -eq 0 ]
    [ "$output" = "${NEW_FILENAME}" ]

    rm ${NEW_FILENAME}
}

@test "rename directory" {
    local DIRECTORY=col.d
    mkdir ${DIRECTORY}
    run ${LS_EXECUTABLE}
    #echo "# status = $status" >&3
    #echo "# output = $output" >&3
    #echo "# DIRECTORY = ${DIRECTORY}" >&3
    [ "$status" -eq 0 ]
    [ "$output" = "${DIRECTORY}" ]

    local NEW_DIRECTORY=renamed.d
    mv ${DIRECTORY} ${NEW_DIRECTORY}

    run ${LS_EXECUTABLE}
    #echo "# status = $status" >&3
    #echo "# output = $output" >&3
    #echo "# NEW_DIRECTORY = ${NEW_DIRECTORY}" >&3
    [ "$status" -eq 0 ]
    [ "$output" = "${NEW_DIRECTORY}" ]

    rmdir ${NEW_DIRECTORY}
}

@test "move file into directory" {
    local FILENAME=file.txt
    touch ${FILENAME}

    local DIRECTORY=col.d
    mkdir ${DIRECTORY}
    mv ${FILENAME} ${DIRECTORY}

    run ${LS_EXECUTABLE} ${DIRECTORY}/${FILENAME}
    [ "$status" -eq 0 ]
    [ "$output" = "${DIRECTORY}/${FILENAME}" ]

    rm ${DIRECTORY}/${FILENAME}
    rmdir ${DIRECTORY}
}

@test "move directory into sibling directory" {
    local DIRECTORY_0=col_0.d
    mkdir ${DIRECTORY_0}

    local DIRECTORY_1=col_1.d
    mkdir ${DIRECTORY_1}

    mv ${DIRECTORY_0} ${DIRECTORY_1}

    run ${LS_EXECUTABLE} ${DIRECTORY_1}
    [ "$status" -eq 0 ]
    [ "$output" = "${DIRECTORY_0}" ]

    rmdir ${DIRECTORY_1}/${DIRECTORY_0}
    rmdir ${DIRECTORY_1}
}

@test "large file transfer" {
    dd if=/dev/zero of=large_file.bin bs=2M count=32
}

@test "copy large file" {
    local FILENAME=large_file.bin
    dd if=/dev/zero of=${FILENAME} bs=2M count=32

    local FILENAME_COPIED=${FILENAME}.copied
    cp ${FILENAME} ${FILENAME_COPIED}
    run ${LS_EXECUTABLE} ${FILENAME_COPIED}
    [ "$status" -eq 0 ]
    [ "$output" = "${FILENAME_COPIED}" ]
}

