# FC4

<figure style="float: right; border: 1px solid silver; padding: 1em; margin-top: 0; text-align: center;">
  <img src="diagrams/fc4-02-container.png"
       width="350" height="299"
       style="border: 1px solid silver;"
       alt="Example: a container diagram of fc4."
       title="Example: a container diagram of fc4.">
  <figcaption>Example: a container diagram of fc4.</figcaption>
</figure>

FC4 is a [_Docs as Code_][docs-as-code] tool that helps software creators and documentarians author
software architecture diagrams using [the C4 model for visualising software architecture][c4-model].

<style>
   li {
     margin-left: -1em;
     padding-left: 0.5em;
   }

   li#builds::marker { content: "🏗"; }
   li#thanks::marker { content: "🙏"; }
   li#origin::marker { content: "💡"; }
</style>

<ul>
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

To get started, we recommend reading [the user manual](manual/).

If you have any questions or feedback please [create an issue][new-issue] and one of the maintainers
will get back to you shortly.

## The Name

FC4 is not ([yet][backronym]) an acronym or initialism; it doesn’t stand for anything — it’s “just”
a name.

The name is a combination of “FC” and “C4” — the former is a reference to Funding Circle, the
originating context of the tool; the latter to Simon Brown’s C4 model, the foundation of the
tool.

## Copyright & License

Copyright © 2018–2019 Funding Circle Ltd.

Distributed under [the BSD 3-Clause License][license].

[backronym]: https://en.wikipedia.org/wiki/Backronym
[c4-model]: https://c4model.com/
[docs-as-code]: https://www.writethedocs.org/guide/docs-as-code/
[license]: https://github.com/FundingCircle/fc4-framework/blob/master/LICENSE
[new-issue]: https://github.com/FundingCircle/fc4-framework/issues/new
