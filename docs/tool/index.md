# fc4-tool

fc4-tool is a [command-line][cli] tool that supports and facilitates working with [FC4](/) diagrams.

While it was initially created to clean up the formatting of diagram source YAML files, its feature
set has expanded over time; it now also “snaps” the elements and vertices in a diagram to a virtual
grid and renders diagrams.

For the backstory of the creation of the tool, see [this blog post][fc4-blog-post].





## Setup

### Quick start for Homebrew users

If you already use [Homebrew][homebrew], one or both of these commands might be  all you need to get
started:

```shell
# If you’re using MacOS and you don’t already have Chromium or Chrome installed:
brew cask install chromium
# If you’re using a different OS and you don’t already have Chromium or Chrome installed
# then install Chromium or Chrome however you generally install such software on your system.

# The main event (should work on any OS that supports Homebrew)
brew install fundingcircle/floss/fc4
```


### Requirements

1. A [Java Runtime Environment (JRE)][adoptopenjdk] or [Java Development Kit (JDK)][adoptopenjdk]
  1. On MacOS if you have [Homebrew](https://brew.sh/) you can run
       `brew cask install adoptopenjdk11-jre`
1. An installation of [Chrome][chrome] or [Chromium][chromium] **70–77** (inclusive)
   1. On MacOS:
      1. If you have [Homebrew](https://brew.sh/) you can run `brew cask install chromium`
      1. Chromium/Chrome must be at either `/Applications/Chromium.app` or
         `/Applications/Google Chrome.app`



### Download and Install

#### With Homebrew

[Homebrew][homebrew] is the recommended installation method for anyone using Linux, MacOS, or
[Windows Subsystem for Linux][wsl]. Please see [Quick start for Homebrew
users](#quick-start-for-homebrew-users) above.

If you don’t already use Homebrew, we recommend you install it and then see [Quick start for
Homebrew users](#quick-start-for-homebrew-users) above.

If you cannot use Homebrew, or would prefer not to, you can [manually](#manually) download and
install the tool:

#### Manually

1. Download the archive for your platform from [the latest release][latest-release]
1. Expand the archive
1. Optional: move the extracted files to somewhere on your `$PATH`
   1. e.g. `mv ~/Downloads/fc4/fc4* ~/bin/`


## Authoring Diagrams

### Abridged Workflow

1. Run in your terminal: `fc4 -fsrw path/to/diagram.yaml/or/dir`
1. Open a YAML file in your text editor and edit it
   1. Either a YAML file specified directly or one in or under a specified directory
1. Whenever you save the file, fc4-tool will see the change, clean up the file (overwriting it) and
   render the diagram to a PNG file (overwriting that file, if it already existed)
1. When you’d like to wrap up your session:
   1. Save the file one last time and wait for fc4-tool to format, snap, and render it
   1. Hit ctrl-c to exit fc4-tool
   1. Run `git status` and you should see that the YAML file has been created/changed and its
      corresponding PNG file has also been created/changed
   1. Commit both files

### Full Workflow

Please see [The Authoring Workflow](../manual/authoring_workflow.html) section of
[the FC4 User Manual](../manual/).





## Source Code

This tool is [Free and Libre Open Source Software (FLOSS)][floss]; its source code is readily
available for review or modification via [its GitHub repository][repo].


[adoptopenjdk]: https://adoptopenjdk.net/installation.html?variant=openjdk11&jvmVariant=hotspot
[chrome]: https://www.google.com/chrome/browser/
[chromium]: https://www.chromium.org/Home
[cli]: https://en.wikipedia.org/wiki/Command-line_interface
[docs-as-code]: https://www.writethedocs.org/guide/docs-as-code/
[fc4-blog-post]: https://engineering.fundingcircle.com/blog/2018/09/07/the-fc4-framework/
[floss]: https://en.wikipedia.org/wiki/Free_and_open-source_software
[homebrew]: https://brew.sh/
[latest-release]: https://github.com/FundingCircle/fc4-framework/releases/latest
[repo]: https://github.com/FundingCircle/fc4-framework
[structurizr-express]: https://structurizr.com/help/express
[wsl]: https://docs.microsoft.com/en-us/windows/wsl/about
