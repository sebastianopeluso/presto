/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.hive;

import com.facebook.presto.hive.filesystem.ExtendedFileSystem;
import com.facebook.presto.hive.metastore.Partition;
import com.facebook.presto.hive.metastore.Table;
import org.apache.hadoop.fs.Path;

import java.util.Iterator;
import java.util.Optional;

public interface DirectoryLister
{
    Iterator<HiveFileInfo> list(
            ExtendedFileSystem fileSystem,
            Table table,
            Path path,
            Optional<Partition> partition,
            NamenodeStats namenodeStats,
            HiveDirectoryContext hiveDirectoryContext);

    default boolean isPathCached(Path path)
    {
        return false;
    }
}
