cmake_minimum_required(VERSION 3.7 FATAL_ERROR)
project(irods_client_nfsrods LANGUAGES)

# Package Contents
# ~~~~~~~~~~~~~~~~
# /etc/nfsrods/exports
# /etc/nfsrods/log4j.properties
# /etc/nfsrods/README
# /etc/nfsrods/server.json
# /opt/irods-clients/nfsrods-1.0.0-SNAPSHOT-jar-with-dependencies.jar
# /etc/init.d/nfsrods

#if (NOT IRODS_LINUX_DISTRIBUTION_NAME)
#    execute_process(COMMAND "python" "-c" "from __future__ import print_function; import platform; print(platform.linux_distribution()[0].split()[0].strip().lower(), end='')"
#                    RESULT_VARIABLE IRODS_EXECUTE_PROCESS_RESULT_LINUX_DISTRIBUTION_NAME
#                    OUTPUT_VARIABLE IRODS_LINUX_DISTRIBUTION_NAME)
#
#    if (NOT ${IRODS_EXECUTE_PROCESS_RESULT_LINUX_DISTRIBUTION_NAME} STREQUAL "0")
#        message(FATAL_ERROR "Linux platform name detection failed\n${IRODS_EXECUTE_PROCESS_RESULT_LINUX_DISTRIBUTION_NAME}")
#    endif()
#
#    set(IRODS_LINUX_DISTRIBUTION_NAME ${IRODS_LINUX_DISTRIBUTION_NAME} CACHE STRING "Linux distribution name, e.g. {ubuntu, centos, ...}." FORCE)
#    message(STATUS "Setting unspecified IRODS_LINUX_DISTRIBUTION_NAME to '${IRODS_LINUX_DISTRIBUTION_NAME}'")
#endif()

set(PACKAGE_VERSION_MAJOR 1)
set(PACKAGE_VERSION_MINOR 0)
set(PACKAGE_VERSION_PATCH 0)
set(PACKAGE_VERSION "${PACKAGE_VERSION_MAJOR}.${PACKAGE_VERSION_MINOR}.${PACKAGE_VERSION_PATCH}")

install(FILES ${CMAKE_SOURCE_DIR}/irods-vfs-impl/target/nfsrods-1.0.0-SNAPSHOT-jar-with-dependencies.jar
        DESTINATION /opt/irods-clients/nfsrods
        PERMISSIONS OWNER_READ OWNER_WRITE GROUP_READ WORLD_READ)

install(FILES ${CMAKE_SOURCE_DIR}/packaging/README
        DESTINATION /etc/nfsrods
        PERMISSIONS OWNER_READ OWNER_WRITE GROUP_READ WORLD_READ)

install(DIRECTORY ${CMAKE_SOURCE_DIR}/irods-vfs-impl/config/
        DESTINATION /etc/nfsrods
        FILES_MATCHING
            PATTERN */exports
            PATTERN */log4j.properties
            PATTERN */server.json)

set(PACKAGE_NAME irods-client-nfsrods)

set(CPACK_PACKAGE_FILE_NAME "${PACKAGE_NAME}-${PACKAGE_VERSION}-${CMAKE_SYSTEM_PROCESSOR}")

set(CPACK_PACKAGE_NAME ${PACKAGE_NAME})
set(CPACK_INCLUDE_TOPLEVEL_DIRECTORY OFF)
set(CPACK_COMPONENT_INCLUDE_TOPLEVEL_DIRECTORY OFF)
set(CPACK_COMPONENTS_GROUPING IGNORE)
set(CPACK_PACKAGE_VERSION ${PACKAGE_VERSION})
set(CPACK_PACKAGE_VERSION_MAJOR ${PACKAGE_VERSION_MAJOR})
set(CPACK_PACKAGE_VERSION_MINOR ${PACKAGE_VERSION_MINOR})
set(CPACK_PACKAGE_VERSION_PATCH ${PACKAGE_VERSION_PATCH})
set(CPACK_PACKAGE_CONTACT "Renaissance Computing Institute <info@irods.org>")
set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "iRODS Client - Allows iRODS to be surfaced as an NFS mount point")
set(CPACK_PACKAGE_VENDOR "Renaissance Computing Institute <info@irods.org>")

set(CPACK_DEB_COMPONENT_INSTALL OFF)
set(CPACK_DEBIAN_PACKAGE_SECTION "contrib/science")
set(CPACK_DEBIAN_COMPRESSION_TYPE "gzip")
set(CPACK_DEBIAN_PACKAGE_PRIORITY "extra")
set(CPACK_DEBIAN_PACKAGE_HOMEPAGE "https://irods.org")
set(CPACK_DEBIAN_PACKAGE_SHLIBDEPS OFF)
set(CPACK_DEBIAN_PACKAGE_CONTROL_STRICT_PERMISSION ON)
set(CPACK_DEBIAN_PACKAGE_BREAKS ${PACKAGE_NAME})
set(CPACK_DEBIAN_PACKAGE_REPLACES ${PACKAGE_NAME})
set(CPACK_DEBIAN_PACKAGE_NAME ${PACKAGE_NAME})
set(CPACK_DEBIAN_PACKAGE_DEPENDS "openjdk-8-jre")

set(CPACK_RPM_COMPONENT_INSTALL OFF)
set(CPACK_RPM_PACKAGE_RELEASE "1")
set(CPACK_RPM_PACKAGE_LICENSE "BSD-3-Clause")
set(CPACK_RPM_PACKAGE_VENDOR "iRODS Consortium")
set(CPACK_RPM_PACKAGE_URL "https://irods.org")
set(CPACK_RPM_PACKAGE_AUTOREQ 0)
set(CPACK_RPM_PACKAGE_AUTOPROV 0)
set(CPACK_RPM_PACKAGE_OBSOLETES ${PACKAGE_NAME})
set(CPACK_RPM_PACKAGE_NAME ${PACKAGE_NAME})
set(CPACK_RPM_PACKAGE_REQUIRES "java-1.8.0-openjdk")

set(CPACK_ARCHIVE_COMPONENT_INSTALL OFF)

include(CPack)
