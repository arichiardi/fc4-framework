# FC4

<style>
  figure {
    float: right;
    border: 1px solid silver;
    padding: 1em;
    text-align: center;

    /* Hides the rule under the headings where it would otherwise appear behing the figure. */
    background-color: white;
  }

  figure img {
    border: 1px solid silver;
    min-width: 350px;
    min-height: 299px;
  }

  figure + p {
    font-size: 125%;
  }

  ul#info { margin-top: 2em; margin-bottom: 2em; }

  ul#info > li {
    margin-left: -0.5em;
    padding-left: 0.5em;
    margin-bottom: 1em;
  }

  li#info::marker { font-size: 150%; }
  li#builds::marker { content: "🏗"; }
  li#thanks::marker { content: "🙏"; }
  li#origin::marker { content: "💡"; }
</style>

<figure>
  <img src="diagrams/fc4-02-container.png" width="350" height="299"
       alt="Example: a container diagram of fc4.">
  <figcaption>Example: a container diagram of fc4.</figcaption>
</figure>

FC4 is a [_Docs as Code_][docs-as-code] tool that helps software creators and documentarians author
software architecture diagrams using [the C4 model for visualising software architecture][c4-model].

<ul id="info">
  <li id="builds">
    It builds on <a href="https://structurizr.com/express">Structurizr Express</a>.
  </li>
  <li id="thanks">
    Many thanks to <a href="http://simonbrown.je/">Simon Brown</a> for creating and maintaining both
    the C4 model and Structurizr Express.
  </li>
  <li id="origin">
    It originated at and is maintained by <a href="https://engineering.fundingcircle.com/">Funding
    Circle</a>.
  </li>
</ul>


## Get Started

To get started, we recommend reading [the user manual](manual/).


## Help & Feedback

If you have any questions or feedback please [create an issue][new-issue] and one of the maintainers
will get back to you shortly.


## Documentation

* [CLI Reference](reference/cli.md)
* [Contributing](contributing.md)
* [Developing and Testing](dev/index.md)
* [The Name](the-name.md)
* [User Manual](manual/index.md)


## Copyright & License

Copyright © 2018–2019 Funding Circle Ltd.

Distributed under [the BSD 3-Clause License][license].

[c4-model]: https://c4model.com/
[docs-as-code]: https://www.writethedocs.org/guide/docs-as-code/
[license]: https://github.com/FundingCircle/fc4-framework/blob/master/LICENSE
[new-issue]: https://github.com/FundingCircle/fc4-framework/issues/new
