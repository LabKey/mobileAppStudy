To build this module as a standalone distribution, you will need to do the following steps, all in the root of your SVN source (the level right above the "server" directory):

1. In your settings.gradle, you will see a commented out line with this text:
    //include ":server:optionalModules:workflow"
Underneath this line, you'll need a line with this text:
    include ":server:optionalModules:mobileAppStudy:distributions:fda"
(And if you haven't already included it, you'll also need a line with this text: include ":server:optionalModules:mobileAppStudy" )
2. On the command line (again, in the root of your SVN source), run this line:
    ./gradlew :server:optionalModules:mobileAppStudy:distributions:fda:dist
3. Look in the directory "dist/mobileAppStudy" for a filename ending in "mobileAppStudy-bin.tar.gz". Install this distribution using the instructions here: https://www.labkey.org/Documentation/wiki-page.view?name=manualInstall