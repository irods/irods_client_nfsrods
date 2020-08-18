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
