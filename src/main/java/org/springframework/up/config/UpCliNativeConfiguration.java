/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.up.config;

import java.util.Base64;

import com.sun.jna.CallbackReference;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHLicense;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHObject;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHVerification;
import org.kohsuke.github.GitUser;

import org.springframework.nativex.hint.FieldHint;
import org.springframework.nativex.hint.JdkProxyHint;
import org.springframework.nativex.hint.MethodHint;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.ResourceHint;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.nativex.type.NativeConfiguration;
import org.springframework.up.initializr.model.ArtifactId;
import org.springframework.up.initializr.model.BootVersion;
import org.springframework.up.initializr.model.Dependencies;
import org.springframework.up.initializr.model.Dependency;
import org.springframework.up.initializr.model.DependencyCategory;
import org.springframework.up.initializr.model.Description;
import org.springframework.up.initializr.model.GroupId;
import org.springframework.up.initializr.model.IdName;
import org.springframework.up.initializr.model.JavaVersion;
import org.springframework.up.initializr.model.Language;
import org.springframework.up.initializr.model.Metadata;
import org.springframework.up.initializr.model.Name;
import org.springframework.up.initializr.model.PackageName;
import org.springframework.up.initializr.model.Packaging;
import org.springframework.up.initializr.model.ProjectType;
import org.springframework.up.initializr.model.Version;
import org.springframework.up.initializr.model.JavaVersion.JavaVersionValues;
import org.springframework.up.initializr.model.Language.LanguageValues;
import org.springframework.up.initializr.model.Packaging.PackagingValues;
import org.springframework.up.initializr.model.ProjectType.ProjectTypeValue;

/**
 * {@code spring-native} configuration.
 *
 * We keep a lot of stuff here for now. When {@code spring-native} is more
 * mature and production ready {@code spring-shell} will have its own
 * native config.
 *
 * @author Janne Valkealahti
 */
@NativeHint(
	options = {
		// https://github.com/rd-1-2022/spring-up/issues/14
		"-H:+AllowJRTFileSystem",
		// Attempt to make win not to fail fast
		"-H:-DeadlockWatchdogExitOnTimeout",
		"-H:DeadlockWatchdogInterval=0"
	},
	resources = {
		@ResourceHint(
			patterns = {
				"completion/.*",
				"template/.*.st",
				"org/springframework/shell/component/.*.stg",
				"com/sun/jna/win32-x86-64/jnidispatch.dll",
				"org/apache/tika/mime/tika-mimetypes.xml"
			}
		),
	},
	types = {
		@TypeHint(
			types = {
				ArtifactId.class, BootVersion.class, Dependencies.class, Dependency.class, DependencyCategory.class,
				Description.class, GroupId.class, IdName.class, JavaVersion.class, JavaVersionValues.class, Language.class,
				LanguageValues.class, Metadata.class, Name.class, PackageName.class, Packaging.class, PackagingValues.class,
				ProjectType.class, ProjectTypeValue.class, Version.class, CallbackReference.class, Native.class,
				NativeLong.class, PointerByReference.class, IntByReference.class, Base64.Decoder.class
			},
			typeNames = { "com.sun.jna.Klass" },
			access = {
				TypeAccess.PUBLIC_CLASSES, TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.PUBLIC_FIELDS,
				TypeAccess.PUBLIC_METHODS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			types = Structure.class,
			fields = {
				@FieldHint( name = "memory", allowWrite = true),
				@FieldHint( name = "typeInfo")
			},
			methods = {
				@MethodHint( name = "newInstance", parameterTypes = { Class.class, Pointer.class }),
				@MethodHint( name = "newInstance", parameterTypes = { Class.class, long.class }),
				@MethodHint( name = "newInstance", parameterTypes = { Class.class })
			},
			access = {
				TypeAccess.PUBLIC_CLASSES, TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.PUBLIC_FIELDS,
				TypeAccess.PUBLIC_METHODS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "com.sun.jna.Structure$FFIType",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "com.sun.jna.Structure$FFIType$size_t",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$CHAR_INFO",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$CONSOLE_CURSOR_INFO",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$CONSOLE_SCREEN_BUFFER_INFO",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$COORD",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$INPUT_RECORD",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$INPUT_RECORD$EventUnion",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$KEY_EVENT_RECORD",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$MOUSE_EVENT_RECORD",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$WINDOW_BUFFER_SIZE_RECORD",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$MENU_EVENT_RECORD",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$FOCUS_EVENT_RECORD",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$SMALL_RECT",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.jline.terminal.impl.jna.win.Kernel32$UnionChar",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.springframework.up.support.UpCliUserConfig$Hosts",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.springframework.up.support.UpCliUserConfig$Host",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			typeNames = "org.kohsuke.github.GitHubInteractiveObject",
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		),
		@TypeHint(
			types = {
				GHMyself.class, GHObject.class, GHPerson.class, GHUser.class, GHCommit.class, GHLicense.class,
				GHRepository.class, GHVerification.class, GitUser.class
			},
			access = {
				TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.DECLARED_CLASSES, TypeAccess.DECLARED_CONSTRUCTORS,
				TypeAccess.DECLARED_FIELDS, TypeAccess.DECLARED_METHODS
			}
		)
	},
	jdkProxies = {
		@JdkProxyHint( typeNames = { "com.sun.jna.Library" }),
		@JdkProxyHint( typeNames = { "com.sun.jna.Callback" }),
		@JdkProxyHint( typeNames = { "org.jline.terminal.impl.jna.win.Kernel32" }),
		@JdkProxyHint( typeNames = { "org.jline.terminal.impl.jna.linux.CLibrary" })
	}
)
public class UpCliNativeConfiguration implements NativeConfiguration {
}
