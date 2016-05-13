major_version: "local"
minor_version: ""
default_target_cpu: "same_as_host"

default_toolchain {
  cpu: "%{cpu}"
  toolchain_identifier: "local"
}

default_toolchain {
  cpu: "armeabi-v7a"
  toolchain_identifier: "stub_armeabi-v7a"
}

# Android tooling requires a default toolchain for the armeabi-v7a cpu.
toolchain {
  abi_version: "armeabi-v7a"
  abi_libc_version: "armeabi-v7a"
  builtin_sysroot: ""
  compiler: "compiler"
  host_system_name: "armeabi-v7a"
  needsPic: true
  supports_gold_linker: false
  supports_incremental_linker: false
  supports_fission: false
  supports_interface_shared_objects: false
  supports_normalizing_ar: false
  supports_start_end_lib: false
  supports_thin_archives: false
  target_libc: "armeabi-v7a"
  target_cpu: "armeabi-v7a"
  target_system_name: "armeabi-v7a"
  toolchain_identifier: "stub_armeabi-v7a"

  tool_path { name: "ar" path: "/bin/false" }
  tool_path { name: "compat-ld" path: "/bin/false" }
  tool_path { name: "cpp" path: "/bin/false" }
  tool_path { name: "dwp" path: "/bin/false" }
  tool_path { name: "gcc" path: "/bin/false" }
  tool_path { name: "gcov" path: "/bin/false" }
  tool_path { name: "ld" path: "/bin/false" }

  tool_path { name: "nm" path: "/bin/false" }
  tool_path { name: "objcopy" path: "/bin/false" }
  tool_path { name: "objdump" path: "/bin/false" }
  tool_path { name: "strip" path: "/bin/false" }
  linking_mode_flags { mode: DYNAMIC }
}

toolchain {
  toolchain_identifier: "local"
%{content}

  compilation_mode_flags {
    mode: DBG
%{dbg_content}
  }
  compilation_mode_flags {
    mode: OPT
%{opt_content}
  }
  linking_mode_flags { mode: DYNAMIC }

  feature {
    name: 'include_paths'
    flag_set {
      action: 'preprocess-assemble'
      action: 'c-compile'
      action: 'c++-compile'
      action: 'c++-header-parsing'
      action: 'c++-header-preprocessing'
      action: 'c++-module-compile'
      flag_group {
        flag: '-iquote'
        flag: '%{quote_include_paths}'
      }
      flag_group {
        flag: '-I%{include_paths}'
      }
      flag_group {
        flag: '-isystem'
        flag: '%{system_include_paths}'
      }
    }
  }

  feature {
    name: 'coverage'
    provides: 'profile'
    flag_set {
      action: 'preprocess-assemble'
      action: 'c-compile'
      action: 'c++-compile'
      action: 'c++-header-parsing'
      action: 'c++-header-preprocessing'
      action: 'c++-module-compile'
      expand_if_all_available: 'gcov_gcno_file'
      flag_group {
        flag: '-fprofile-arcs'
        flag: '-ftest-coverage'
      }
    }
    flag_set {
      action: 'c++-link'
      flag_group {
        flag: '-lgcov'
      }
    }
  }

  feature {
    name: 'fdo_instrument'
    provides: 'profile'
    flag_set {
      action: 'c-compile'
      action: 'c++-compile'
      action: 'c++-link'
      flag_group {
%{fdo_instrument_flag_content}
      }
      flag_group {
        flag: '-fno-data-sections'
      }
    }
  }

  feature {
    name: 'fdo_optimize'
    provides: 'profile'
    flag_set {
      action: 'c-compile'
      action: 'c++-compile'
      expand_if_all_available: 'fdo_profile_path'
      flag_group {
%{fdo_optimize_flag_content}
        flag: '-fprofile-correction'
      }
    }
  }

  feature {
    name: 'autofdo'
    provides: 'profile'
    flag_set {
      action: 'c-compile'
      action: 'c++-compile'
      expand_if_all_available: 'fdo_profile_path'
      flag_group {
        flag: '-fauto-profile=%{fdo_profile_path}'
        flag: '-fprofile-correction'
      }
    }
  }

  feature {
    name: 'lipo'
    requires { feature: 'autofdo' }
    requires { feature: 'fdo_optimize' }
    requires { feature: 'fdo_instrument' }
    flag_set {
      action: 'c-compile'
      action: 'c++-compile'
      flag_group {
        flag: '-fripa'
      }
    }
  }
}
