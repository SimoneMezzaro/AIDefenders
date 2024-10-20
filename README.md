# AI Defenders

AI Defenders is a smart assistant developed as an integration for the Code Defenders mutation testing game. AI Defenders
leverages GPT models and chat completion API by OpenAI to assist students playing games of Code Defenders.

AI Defenders has been developed to study how software testing students interact with GPT-based tools and how the power
of Large Language Models can impact learning and practice of software testing. The results of the study have been
presented at the LEARNER workshop run in the context of the EASE 2024 conference and have been published in the paper
[An Empirical Study on How Large Language Models Impact Software Testing Learning](https://dl.acm.org/doi/10.1145/3661167.3661273).

## How to Run AI Defenders

An instance of Code Defenders including the AI Defenders assistant can be easily locally deployed using Docker!

### Configuration

Since AI Defenders relies on OpenAI API to provide its assistance, it requires to specify an OpenAI token key and the
GPT model to be used. Both properties can be specified in the [.env](docker/.env) file.

The API token can be retrieved from you account on the [OpenAI website](https://platform.openai.com/api-keys) and can be
added as `CODEDEFENDERS_OPENAI_API_KEY`.

The GPT model can instead be selected among the [currently available models](https://platform.openai.com/docs/models)
and can be added as `CODEDEFENDERS_OPENAI_CHATGPT_MODEL`.

Refer to the [original Code Defenders Docker documentation](docker/README.md) for editing additional properties in the
[.env](docker/.env) file.

### Deployment

To locally deploy the platform move to the root of the AI Defenders repository and run the following commands:
```sh
docker build --file ./docker/Dockerfile --tag localhost/aidefenders:aid .
cd docker
docker-compose up
```
