############################################
# NFSRODS bats test file
# - https://github.com/bats-core/bats-core
#
# From within an NFSRODS mountpoint, run:
# $ bats test_nfsrods.bats
############################################

@test "create, list, and remove file" {
    FILENAME=removeme

    run echo ${FILENAME} > ${FILENAME}
    [ $status -eq 0 ]

    run ls ${FILENAME}
    [ $status -eq 0 ]
    [ "${lines[0]}" = "${FILENAME}" ]

    run rm ${FILENAME}
    [ $status -eq 0 ]

    run ls ${FILENAME}
    [ $status -eq 2 ]
    [[ "${lines[0]}" =~ "ls: cannot access" ]]
    [[ "${lines[0]}" =~ "${FILENAME}" ]]
    [[ "${lines[0]}" =~ "No such file or directory" ]]
}

@test "listing directory with large number of entries does not trigger duplicate cookie error" {
    SANDBOX='dup_cookie_sandbox'

    run mkdir -p ${SANDBOX}
    [ $status -eq 0 ]

    run mkdir -p ${SANDBOX}/c{001..125}
    [ $status -eq 0 ]

    run ls ${SANDBOX}
    [[ ! $(dmesg | tail | grep 'has duplicate cookie') ]]

    run rm -rf ${SANDBOX}
    [ $status -eq 0 ]
}

@test "listing directory with large number of entries prints all entries" {
    SANDBOX='large_directory_sandbox'

    mkdir -p ${SANDBOX}

    run parallel touch ::: ${SANDBOX}/foo{0001..3000}

    result="$(ls ${SANDBOX} | wc -l)"
    [ "$result" -eq 3000 ]

    run parallel rm ::: ${SANDBOX}/foo{0001..3000}
    rm -rf ${SANDBOX}
}

