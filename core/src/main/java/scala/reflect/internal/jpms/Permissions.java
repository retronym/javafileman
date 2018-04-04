//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.FileStore;
//import java.nio.file.FileSystems;
//import java.nio.file.Files;
//import java.nio.file.attribute.*;
//import java.util.*;
//
//import static java.nio.file.attribute.AclEntryPermission.*;
//import static java.nio.file.attribute.AclEntryType.ALLOW;
//
//public class Permissions {
//    public static void main(String[] args) throws IOException {
//        File dir = new File(new File(System.getProperty("user.home")), "dir");
//
//        File file = new File(dir, "file");
//        if (file.exists()) {
//            file.delete();
//        }
//        if (dir.exists()){
//            dir.delete();
//        }
//        dir.mkdirs();
//        file.createNewFile();
//
//        FileStore fileStore = Files.getFileStore(dir.toPath());
//        if (fileStore.supportsFileAttributeView(AclFileAttributeView.class)) {
//            AclFileAttributeView aclView = Files.getFileAttributeView(file.toPath(), AclFileAttributeView.class);
//            AclEntry.Builder builder = AclEntry.newBuilder();
//            builder.setType(ALLOW);
//            String ownerName = System.getProperty("user.name");
//            UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
//            UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(ownerName);
//            builder.setPrincipal(userPrincipal);
//            builder.setPermissions(READ_DATA, WRITE_DATA, APPEND_DATA, READ_NAMED_ATTRS, READ_ATTRIBUTES, WRITE_NAMED_ATTRS, WRITE_ATTRIBUTES, DELETE, READ_ACL, SYNCHRONIZE);
//            AclEntry entry = builder.build();
//            // It might be preferable to just remove the ACL
//            aclView.setAcl(Collections.singletonList(entry));
//        } else if (fileStore.supportsFileAttributeView(PosixFileAttributeView.class)) {
//            PosixFileAttributeView posixView = Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class);
//            HashSet<PosixFilePermission> posixFilePermissions = new HashSet<>();
//            posixFilePermissions.add(PosixFilePermission.OWNER_READ);
//            posixFilePermissions.add(PosixFilePermission.OWNER_WRITE);
//            posixView.setPermissions(posixFilePermissions);
//        } else {
//            boolean isWin = System.getProperty("os.name").startsWith("Windows");
//            if (isWin) {
//                File icalcExe = new File(new File(System.getProperty("SystemRoot"), "System32"), "icacls.exe");
//                if (icalcExe.exists()) {
//                    new ProcessBuilder(icalcExe.getAbsolutePath(), "/")
//                }
//            } else {
//                file.setWritable(false);
//                file.setWritable(true, true);
//                file.setReadable(false);
//                file.setReadable(true, true);
//                if (file.isDirectory()) {
//                    file.setExecutable(false);
//                    file.setExecutable(true, true);
//                }
//            }
//        }
//    }
//}
