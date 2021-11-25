#!/bin/tcsh
#



#set cmnd=`date`


set export_plugins=`find ../plugins/Exporter -name "*.html" -print | sort`
set import_plugins=`find ../plugins/Importer -name "*.html" -print | sort`
set transform_plugins=`find ../plugins/Transform -name "*.html" -print | sort`
set filter_plugins=`find ../plugins/Filter -name "*.html" -print | sort`
set viewer_plugins=`find ../plugins/Viewer -name "*.html" -print | sort`

set display_menu="manual/DisplayMenuHead.html ViewerColours.html ViewerFind.html ViewerLayout.html NewView.html ViewerPrint.html ApplyFilter.html"

set tutorials="Started.html ClusterTutorial.html CommandTutorial.html ClusterAPITutorial.html PluginTutorial.html RunningCommands.html RMIInterface.html WrappingTutorial.html"

set sundry="AnnotationLoader.html AnnotationViewer.html NameTagEditor.html DatabaseConnection.html Merging.html maxdViewISYS.html DataModel.html FileFormats.html"

#set htmldoc="c:\\softs\\htmldoc\\htmldoc.exe"
set htmldoc="/cygdrive/c/softs/htmldoc/htmldoc.exe"
set htmldoc_args="--datadir C:\\softs\\htmldoc"

$htmldoc $htmldoc_args -t pdf -f maxdViewUserGuide.pdf --fontsize 10 --headfootsize 7 --book --toclevels 2 --tocheader "..." --header "c.." --footer "h.1" --compression=5 --firstpage toc --size A4 --numbered --titlefile manual/Foreword.html Overview.html Concepts.html Plugins.html Popup.html CustomMenu.html SelectionMenu.html manual/FileMenuHead.html $export_plugins  $import_plugins manual/TransformMenuHead.html $transform_plugins  manual/FilterMenuHead.html Filters.html $filter_plugins $display_menu  manual/ViewerMenuHead.html $viewer_plugins $sundry $tutorials Glossary.html 

#htmldoc -t pdf -f maxdViewRef.pdf --book manual/RefForeword.html PluginCommandsList.html MethodRef.html ProgGuide.html

ls -l *.pdf
