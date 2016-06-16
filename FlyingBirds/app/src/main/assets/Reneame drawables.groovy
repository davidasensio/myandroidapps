import groovy.io.FileType

def list = []
def folder = new File("C:\\tmp\\and");
folder.eachFileRecurse (FileType.FILES) { file ->
    def fileName = file.getAbsolutePath();
    def dotPosition = fileName.lastIndexOf(".");
    def extension = ""
    def newName = ""

    if (dotPosition >= 0) {
        extension = fileName.substring(dotPosition);
    }

    def info = fileName.substring(11);
    info = info.substring(0, info.indexOf("\\"));
    info = info + ".png"

    if (extension.toUpperCase() == ".PNG") {
        newName = fileName.substring(0, fileName.lastIndexOf("\\") + 1)
        newName = newName + info;
        //println "$info - $newName"
    }


    file.renameTo(new File(newName))
    list << newName
}
list.each {
    println it
}